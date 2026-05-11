package com.eneserdogan.videobolucu;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.SparseIntArray;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VideoSplitter {
    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024 * 1024;
    private static final String OUTPUT_FOLDER = "VideoBolucu";

    private VideoSplitter() {
    }

    public interface ProgressCallback {
        void onProgress(int completedSegments, int totalSegments, String message);
    }

    public static final class ClipResult {
        public final Uri uri;
        public final String displayName;

        ClipResult(Uri uri, String displayName) {
            this.uri = uri;
            this.displayName = displayName;
        }
    }

    private static final class OutputTarget implements AutoCloseable {
        final Uri uri;
        final String displayName;
        final ParcelFileDescriptor fileDescriptor;

        OutputTarget(Uri uri, String displayName, ParcelFileDescriptor fileDescriptor) {
            this.uri = uri;
            this.displayName = displayName;
            this.fileDescriptor = fileDescriptor;
        }

        @Override
        public void close() throws IOException {
            fileDescriptor.close();
        }
    }

    public static List<ClipResult> split(
            Context context,
            Uri sourceUri,
            int segmentSeconds,
            ProgressCallback callback
    ) throws IOException {
        if (segmentSeconds <= 0) {
            throw new IllegalArgumentException("Parça süresi 1 saniye veya daha büyük olmalı.");
        }

        long durationUs = readDurationUs(context, sourceUri);
        if (durationUs <= 0L) {
            throw new IOException("Video süresi okunamadı.");
        }

        long segmentUs = segmentSeconds * 1_000_000L;
        int totalSegments = (int) Math.ceil(durationUs / (double) segmentUs);
        String sourceName = readDisplayName(context, sourceUri);
        String baseName = sanitizeBaseName(sourceName);
        int rotation = readRotation(context, sourceUri);

        List<ClipResult> results = new ArrayList<>();
        for (int index = 0; index < totalSegments; index++) {
            long startUs = index * segmentUs;
            long endUs = Math.min(durationUs, startUs + segmentUs);
            String displayName = String.format(Locale.US, "%s_part_%03d.mp4", baseName, index + 1);

            if (callback != null) {
                callback.onProgress(index, totalSegments, displayName + " hazırlanıyor");
            }

            OutputTarget target = null;
            boolean success = false;
            try {
                target = createOutputTarget(context, displayName);
                copySegment(context, sourceUri, target, startUs, endUs, rotation);
                publishOutput(context, target.uri);
                results.add(new ClipResult(target.uri, target.displayName));
                success = true;
            } finally {
                if (target != null) {
                    try {
                        target.close();
                    } catch (IOException ignored) {
                    }
                    if (!success) {
                        context.getContentResolver().delete(target.uri, null, null);
                    }
                }
            }

            if (callback != null) {
                callback.onProgress(index + 1, totalSegments, displayName + " kaydedildi");
            }
        }

        return results;
    }

    private static void copySegment(
            Context context,
            Uri sourceUri,
            OutputTarget target,
            long startUs,
            long endUs,
            int rotation
    ) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaMuxer muxer = null;
        try {
            extractor.setDataSource(context, sourceUri, null);

            SparseIntArray trackMap = new SparseIntArray();
            int maxInputSize = DEFAULT_BUFFER_SIZE;
            muxer = new MediaMuxer(
                    target.fileDescriptor.getFileDescriptor(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            );
            if (rotation == 90 || rotation == 180 || rotation == 270) {
                muxer.setOrientationHint(rotation);
            }

            int trackCount = extractor.getTrackCount();
            for (int trackIndex = 0; trackIndex < trackCount; trackIndex++) {
                MediaFormat format = extractor.getTrackFormat(trackIndex);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime == null || (!mime.startsWith("video/") && !mime.startsWith("audio/"))) {
                    continue;
                }
                extractor.selectTrack(trackIndex);
                int muxerTrackIndex = muxer.addTrack(format);
                trackMap.put(trackIndex, muxerTrackIndex);
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    maxInputSize = Math.max(maxInputSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE));
                }
            }

            if (trackMap.size() == 0) {
                throw new IOException("Desteklenen video veya ses izi bulunamadi.");
            }

            ByteBuffer buffer = ByteBuffer.allocate(maxInputSize);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            muxer.start();

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            long firstSampleTimeUs = -1L;
            while (true) {
                int sourceTrackIndex = extractor.getSampleTrackIndex();
                if (sourceTrackIndex < 0) {
                    break;
                }

                int muxerTrackIndex = trackMap.get(sourceTrackIndex, -1);
                if (muxerTrackIndex < 0) {
                    extractor.advance();
                    continue;
                }

                long sampleTimeUs = extractor.getSampleTime();
                if (sampleTimeUs < 0 || sampleTimeUs > endUs) {
                    break;
                }

                buffer.clear();
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    break;
                }

                if (firstSampleTimeUs < 0) {
                    firstSampleTimeUs = sampleTimeUs;
                }

                int sampleFlags = mapSampleFlags(extractor.getSampleFlags());
                bufferInfo.set(
                        0,
                        sampleSize,
                        Math.max(0L, sampleTimeUs - firstSampleTimeUs),
                        sampleFlags
                );
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo);
                extractor.advance();
            }
        } finally {
            if (muxer != null) {
                muxer.release();
            }
            extractor.release();
        }
    }

    private static int mapSampleFlags(int extractorFlags) throws IOException {
        if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_ENCRYPTED) != 0) {
            throw new IOException("Şifrelenmiş video örnekleri desteklenmiyor.");
        }

        int sampleFlags = 0;
        if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
            sampleFlags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
        }
        if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
            sampleFlags |= MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
        }
        return sampleFlags;
    }

    private static OutputTarget createOutputTarget(Context context, String displayName) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000L);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/" + OUTPUT_FOLDER);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        } else {
            File directory = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    OUTPUT_FOLDER
            );
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Çıktı klasörü oluşturulamadı: " + directory.getAbsolutePath());
            }
            File outputFile = new File(directory, displayName);
            values.put(MediaStore.MediaColumns.DATA, outputFile.getAbsolutePath());
        }

        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Uri uri = resolver.insert(collection, values);
        if (uri == null) {
            throw new IOException("Çıktı dosyası oluşturulamadı.");
        }

        ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "rw");
        if (pfd == null) {
            resolver.delete(uri, null, null);
            throw new IOException("Çıktı dosyası açılamadı.");
        }

        return new OutputTarget(uri, displayName, pfd);
    }

    private static void publishOutput(Context context, Uri outputUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            context.getContentResolver().update(outputUri, values, null, null);
        }
    }

    private static long readDurationUs(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            String durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (TextUtils.isEmpty(durationMs)) {
                return 0L;
            }
            return Long.parseLong(durationMs) * 1000L;
        } catch (RuntimeException ex) {
            return 0L;
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException | IOException ignored) {
            }
        }
    }

    private static int readRotation(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if (TextUtils.isEmpty(rotation)) {
                return 0;
            }
            return Integer.parseInt(rotation);
        } catch (RuntimeException ex) {
            return 0;
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException | IOException ignored) {
            }
        }
    }

    private static String readDisplayName(Context context, Uri uri) {
        String displayName = null;
        try (Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    displayName = cursor.getString(index);
                }
            }
        } catch (RuntimeException ignored) {
        }

        if (TextUtils.isEmpty(displayName)) {
            displayName = "video";
        }
        return displayName;
    }

    private static String sanitizeBaseName(String displayName) {
        int dotIndex = displayName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? displayName.substring(0, dotIndex) : displayName;
        baseName = baseName.replaceAll("[^A-Za-z0-9._-]+", "_");
        baseName = baseName.replaceAll("_+", "_");
        if (TextUtils.isEmpty(baseName) || "_".equals(baseName)) {
            return "video";
        }
        return baseName;
    }
}
