package com.eneserdogan.videobolucu;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQUEST_PICK_VIDEO = 1001;
    private static final int REQUEST_WRITE_PERMISSION = 1002;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ArrayList<VideoSplitter.ClipResult> clipResults = new ArrayList<>();

    private Uri selectedVideoUri;
    private String selectedVideoName;
    private boolean splitAfterPermission;

    private TextView selectedVideoText;
    private TextView statusText;
    private RadioGroup durationGroup;
    private EditText customDurationInput;
    private Button splitButton;
    private Button shareAllButton;
    private ProgressBar progressBar;
    private LinearLayout resultList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContentView());
        updateSelectedVideo(null, null);
        updateShareAllState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }

    private View buildContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(24));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(this);
        title.setText(R.string.screen_title);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF1E2933);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.subtitle);
        subtitle.setTextSize(15);
        subtitle.setTextColor(0xFF4A5562);
        subtitle.setPadding(0, dp(8), 0, dp(18));
        root.addView(subtitle, matchWrap());

        Button pickButton = primaryButton(getString(R.string.action_pick_video));
        pickButton.setOnClickListener(view -> openVideoPicker());
        root.addView(pickButton, matchWrap());

        selectedVideoText = new TextView(this);
        selectedVideoText.setTextSize(14);
        selectedVideoText.setTextColor(0xFF425466);
        selectedVideoText.setPadding(0, dp(12), 0, dp(20));
        root.addView(selectedVideoText, matchWrap());

        TextView durationTitle = sectionTitle(getString(R.string.segment_duration_title));
        root.addView(durationTitle, matchWrap());

        durationGroup = new RadioGroup(this);
        durationGroup.setOrientation(RadioGroup.VERTICAL);
        durationGroup.setPadding(0, dp(8), 0, dp(8));
        addDurationOption(30, getString(R.string.duration_30));
        addDurationOption(60, getString(R.string.duration_60));
        addDurationOption(90, getString(R.string.duration_90));

        RadioButton customRadio = new RadioButton(this);
        customRadio.setId(View.generateViewId());
        customRadio.setText(R.string.duration_custom);
        customRadio.setTag(0);
        customRadio.setTextSize(16);
        durationGroup.addView(customRadio);
        durationGroup.check(durationGroup.getChildAt(0).getId());
        root.addView(durationGroup, matchWrap());

        customDurationInput = new EditText(this);
        customDurationInput.setHint(R.string.duration_custom_hint);
        customDurationInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        customDurationInput.setSingleLine(true);
        customDurationInput.setText(R.string.default_custom_seconds);
        customDurationInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(customDurationInput, matchWrap());

        splitButton = primaryButton(getString(R.string.action_split_save));
        splitButton.setOnClickListener(view -> startSplitWithPermissionCheck());
        LinearLayout.LayoutParams splitParams = matchWrap();
        splitParams.topMargin = dp(16);
        root.addView(splitButton, splitParams);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = matchWrap();
        progressParams.topMargin = dp(16);
        root.addView(progressBar, progressParams);

        statusText = new TextView(this);
        statusText.setTextSize(14);
        statusText.setTextColor(0xFF425466);
        statusText.setPadding(0, dp(10), 0, dp(18));
        root.addView(statusText, matchWrap());

        LinearLayout resultHeader = new LinearLayout(this);
        resultHeader.setOrientation(LinearLayout.HORIZONTAL);
        resultHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView resultsTitle = sectionTitle(getString(R.string.saved_segments_title));
        resultHeader.addView(resultsTitle, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));
        shareAllButton = secondaryButton(getString(R.string.action_share_all));
        shareAllButton.setOnClickListener(view -> shareAll());
        resultHeader.addView(shareAllButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(resultHeader, matchWrap());

        resultList = new LinearLayout(this);
        resultList.setOrientation(LinearLayout.VERTICAL);
        resultList.setPadding(0, dp(8), 0, 0);
        root.addView(resultList, matchWrap());

        return scrollView;
    }

    private void addDurationOption(int seconds, String label) {
        RadioButton radioButton = new RadioButton(this);
        radioButton.setId(View.generateViewId());
        radioButton.setText(label);
        radioButton.setTag(seconds);
        radioButton.setTextSize(16);
        durationGroup.addView(radioButton);
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_PICK_VIDEO);
    }

    private void startSplitWithPermissionCheck() {
        if (selectedVideoUri == null) {
            Toast.makeText(this, R.string.toast_pick_video_first, Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            splitAfterPermission = true;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
            return;
        }

        startSplit();
    }

    private void startSplit() {
        int segmentSeconds = readSelectedSegmentSeconds();
        if (segmentSeconds <= 0) {
            Toast.makeText(this, R.string.toast_valid_seconds, Toast.LENGTH_SHORT).show();
            return;
        }

        setWorking(true);
        clipResults.clear();
        resultList.removeAllViews();
        updateShareAllState();
        progressBar.setProgress(0);
        statusText.setText(R.string.status_started);

        executorService.execute(() -> {
            try {
                List<VideoSplitter.ClipResult> results = VideoSplitter.split(
                        this,
                        selectedVideoUri,
                        segmentSeconds,
                        (completed, total, message) -> mainHandler.post(() -> {
                            int progress = total == 0 ? 0 : Math.round((completed * 100f) / total);
                            progressBar.setProgress(progress);
                            statusText.setText(getString(R.string.status_progress, message, completed, total));
                        })
                );

                mainHandler.post(() -> {
                    clipResults.addAll(results);
                    renderResults();
                    setWorking(false);
                    progressBar.setProgress(100);
                    statusText.setText(R.string.status_completed);
                    updateShareAllState();
                });
            } catch (Exception ex) {
                mainHandler.post(() -> {
                    setWorking(false);
                    statusText.setText(getString(R.string.status_error, safeMessage(ex)));
                    Toast.makeText(this, R.string.toast_split_failed, Toast.LENGTH_LONG).show();
                    updateShareAllState();
                });
            }
        });
    }

    private int readSelectedSegmentSeconds() {
        int checkedId = durationGroup.getCheckedRadioButtonId();
        RadioButton checkedButton = durationGroup.findViewById(checkedId);
        if (checkedButton != null && checkedButton.getTag() instanceof Integer) {
            int seconds = (Integer) checkedButton.getTag();
            if (seconds > 0) {
                return seconds;
            }
        }

        String customValue = customDurationInput.getText().toString().trim();
        if (TextUtils.isEmpty(customValue)) {
            return 0;
        }

        try {
            int seconds = Integer.parseInt(customValue);
            return seconds > 0 ? seconds : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void renderResults() {
        resultList.removeAllViews();
        for (VideoSplitter.ClipResult result : clipResults) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(7), 0, dp(7));

            TextView name = new TextView(this);
            name.setText(result.displayName);
            name.setTextSize(14);
            name.setTextColor(0xFF1E2933);
            row.addView(name, new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
            ));

            Button shareButton = secondaryButton(getString(R.string.action_share));
            shareButton.setOnClickListener(view -> shareSingle(result));
            row.addView(shareButton, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            resultList.addView(row, matchWrap());
        }
    }

    private void shareSingle(VideoSplitter.ClipResult result) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/mp4");
        intent.putExtra(Intent.EXTRA_STREAM, result.uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.share_single_title)));
    }

    private void shareAll() {
        if (clipResults.isEmpty()) {
            return;
        }

        ArrayList<Uri> uris = new ArrayList<>();
        ClipData clipData = null;
        for (int i = 0; i < clipResults.size(); i++) {
            Uri uri = clipResults.get(i).uri;
            uris.add(uri);
            if (clipData == null) {
                clipData = ClipData.newUri(getContentResolver(), "video", uri);
            } else {
                clipData.addItem(new ClipData.Item(uri));
            }
        }

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("video/mp4");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (clipData != null) {
            intent.setClipData(clipData);
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_all_title)));
    }

    private void setWorking(boolean working) {
        splitButton.setEnabled(!working);
        progressBar.setVisibility(working || progressBar.getProgress() > 0 ? View.VISIBLE : View.GONE);
    }

    private void updateSelectedVideo(Uri uri, String name) {
        selectedVideoUri = uri;
        selectedVideoName = name;
        if (uri == null) {
            selectedVideoText.setText(R.string.selected_video_none);
        } else {
            selectedVideoText.setText(getString(R.string.selected_video_value, selectedVideoName));
        }
    }

    private void updateShareAllState() {
        shareAllButton.setEnabled(!clipResults.isEmpty());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_VIDEO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
            }

            updateSelectedVideo(uri, readDisplayName(uri));
            statusText.setText(R.string.status_video_selected);
            clipResults.clear();
            resultList.removeAllViews();
            updateShareAllState();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted && splitAfterPermission) {
                splitAfterPermission = false;
                startSplit();
            } else {
                splitAfterPermission = false;
                Toast.makeText(this, R.string.toast_storage_permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    private String readDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String displayName = cursor.getString(index);
                    if (!TextUtils.isEmpty(displayName)) {
                        return displayName;
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
        return getString(R.string.default_video_name);
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundColor(0xFF2166A5);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(0xFF2166A5);
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        return button;
    }

    private TextView sectionTitle(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(18);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(0xFF1E2933);
        return textView;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return TextUtils.isEmpty(message) ? ex.getClass().getSimpleName() : message;
    }
}
