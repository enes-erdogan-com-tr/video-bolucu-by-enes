import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public final class GenerateStoreAssets {
    private static final Color BRAND = new Color(11, 110, 143);
    private static final Color BRAND_DARK = new Color(8, 78, 101);
    private static final Color ACCENT = new Color(243, 178, 59);
    private static final Color INK = new Color(30, 41, 51);
    private static final Color MUTED = new Color(74, 85, 98);
    private static final Color SURFACE = new Color(247, 248, 249);
    private static final Color LINE = new Color(220, 227, 232);

    private static final String APP_NAME = "Video B\u00f6l\u00fcc\u00fc by Enes";
    private static final String STORE_TAGLINE = "Videolar\u0131 saniyeler i\u00e7inde b\u00f6l";
    private static final File SOURCE_ICON = new File("store-assets/source/video-bolucu-by-enes-source.png");

    private GenerateStoreAssets() {
    }

    public static void main(String[] args) throws Exception {
        File root = new File("store-assets");
        File iconDir = new File(root, "icon");
        File featureDir = new File(root, "feature-graphic");
        File phoneDir = new File(root, "screenshots/phone");
        mkdirs(iconDir);
        mkdirs(featureDir);
        mkdirs(phoneDir);

        BufferedImage sourceIcon = buildIconFromSource(512);
        save(sourceIcon, new File(iconDir, "video-bolucu-by-enes-512.png"));
        writeAndroidLauncherIcons(sourceIcon);

        save(drawFeatureGraphic(sourceIcon), new File(featureDir, "video-bolucu-feature-1024x500.png"));
        save(drawPhoneHome(), new File(phoneDir, "phone-01-home.png"));
        save(drawPhoneCustomDuration(), new File(phoneDir, "phone-02-custom-duration.png"));
        save(drawPhoneProcessing(), new File(phoneDir, "phone-03-processing.png"));
        save(drawPhoneResults(), new File(phoneDir, "phone-04-results.png"));
    }

    private static BufferedImage buildIconFromSource(int size) throws IOException {
        if (!SOURCE_ICON.exists()) {
            throw new IOException("Source icon is missing: " + SOURCE_ICON.getAbsolutePath());
        }

        BufferedImage source = ImageIO.read(SOURCE_ICON);
        Rectangle bounds = alphaBounds(source, 8);
        if (bounds.isEmpty()) {
            throw new IOException("Source icon has no visible pixels: " + SOURCE_ICON.getAbsolutePath());
        }

        int padding = Math.round(Math.max(bounds.width, bounds.height) * 0.035f);
        int side = Math.max(bounds.width, bounds.height) + (padding * 2);
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;
        int cropX = centerX - side / 2;
        int cropY = centerY - side / 2;

        BufferedImage square = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = square.createGraphics();
        quality(g);
        g.setComposite(AlphaComposite.Src);
        g.drawImage(source, -cropX, -cropY, null);
        g.dispose();

        return resize(square, size, size);
    }

    private static Rectangle alphaBounds(BufferedImage image, int alphaThreshold) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xff;
                if (alpha > alphaThreshold) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return new Rectangle();
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static void writeAndroidLauncherIcons(BufferedImage sourceIcon) throws IOException {
        Map<String, Integer> sizes = new LinkedHashMap<>();
        sizes.put("mipmap-mdpi", 48);
        sizes.put("mipmap-hdpi", 72);
        sizes.put("mipmap-xhdpi", 96);
        sizes.put("mipmap-xxhdpi", 144);
        sizes.put("mipmap-xxxhdpi", 192);

        File resDir = new File("app/src/main/res");
        for (Map.Entry<String, Integer> entry : sizes.entrySet()) {
            File dir = new File(resDir, entry.getKey());
            mkdirs(dir);
            BufferedImage icon = resize(sourceIcon, entry.getValue(), entry.getValue());
            save(icon, new File(dir, "ic_launcher.png"));
            save(icon, new File(dir, "ic_launcher_round.png"));
        }
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        quality(g);
        g.setComposite(AlphaComposite.Src);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return image;
    }

    private static BufferedImage drawFeatureGraphic(BufferedImage sourceIcon) {
        BufferedImage image = new BufferedImage(1024, 500, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        quality(g);
        g.setPaint(new GradientPaint(0, 0, BRAND_DARK, 1024, 500, new Color(19, 152, 176)));
        g.fillRect(0, 0, 1024, 500);
        g.setColor(new Color(255, 255, 255, 35));
        g.fillOval(760, -120, 360, 360);
        g.fillOval(720, 300, 250, 250);

        g.drawImage(sourceIcon, 650, 82, 320, 320, null);

        g.setColor(Color.WHITE);
        drawText(g, APP_NAME, 70, 150, 590, 48, Font.BOLD);
        g.setColor(new Color(232, 248, 252));
        drawWrapped(g, STORE_TAGLINE, 70, 265, 560, 34, Font.PLAIN, 1.2f);

        g.setColor(ACCENT);
        g.fillRoundRect(70, 365, 315, 62, 18, 18);
        g.setColor(new Color(23, 39, 48));
        drawText(g, "30 \u2022 60 \u2022 90 \u2022 \u00d6zel s\u00fcre", 98, 404, 270, 25, Font.BOLD);
        g.dispose();
        return image;
    }

    private static BufferedImage drawPhoneHome() {
        BufferedImage image = phoneCanvas();
        Graphics2D g = image.createGraphics();
        quality(g);
        drawStatusBars(g);
        int y = drawAppHeader(g, 118);
        drawButton(g, 72, y + 12, 936, 132, "Video Se\u00e7", true);
        y += 190;
        drawText(g, "Se\u00e7ili video yok.", 72, y + 48, 900, 34, Font.PLAIN, MUTED);
        y += 138;
        drawText(g, "Par\u00e7a s\u00fcresi", 72, y, 900, 40, Font.BOLD, INK);
        y += 78;
        drawRadio(g, 92, y, true, "30 saniye");
        drawRadio(g, 92, y + 96, false, "60 saniye");
        drawRadio(g, 92, y + 192, false, "90 saniye");
        drawRadio(g, 92, y + 288, false, "\u00d6zel s\u00fcre");
        drawInput(g, 72, y + 420, 936, "45");
        drawButton(g, 72, y + 560, 936, 132, "B\u00f6l ve Kaydet", true);
        drawResultsHeader(g, y + 875, false);
        drawNavBar(g);
        g.dispose();
        return image;
    }

    private static BufferedImage drawPhoneCustomDuration() {
        BufferedImage image = phoneCanvas();
        Graphics2D g = image.createGraphics();
        quality(g);
        drawStatusBars(g);
        int y = drawAppHeader(g, 118);
        drawButton(g, 72, y + 12, 936, 132, "Video Se\u00e7", true);
        y += 190;
        drawText(g, "Se\u00e7ili video: aile-videosu.mp4", 72, y + 48, 900, 34, Font.PLAIN, MUTED);
        y += 138;
        drawText(g, "Par\u00e7a s\u00fcresi", 72, y, 900, 40, Font.BOLD, INK);
        y += 78;
        drawRadio(g, 92, y, false, "30 saniye");
        drawRadio(g, 92, y + 96, false, "60 saniye");
        drawRadio(g, 92, y + 192, false, "90 saniye");
        drawRadio(g, 92, y + 288, true, "\u00d6zel s\u00fcre");
        drawInput(g, 72, y + 420, 936, "75");
        drawButton(g, 72, y + 560, 936, 132, "B\u00f6l ve Kaydet", true);
        drawText(g, "75 saniyelik par\u00e7alar haz\u0131r.", 72, y + 750, 900, 32, Font.PLAIN, MUTED);
        drawNavBar(g);
        g.dispose();
        return image;
    }

    private static BufferedImage drawPhoneProcessing() {
        BufferedImage image = phoneCanvas();
        Graphics2D g = image.createGraphics();
        quality(g);
        drawStatusBars(g);
        int y = drawAppHeader(g, 118);
        drawButton(g, 72, y + 12, 936, 132, "Video Se\u00e7", true);
        y += 190;
        drawText(g, "Se\u00e7ili video: aile-videosu.mp4", 72, y + 48, 900, 34, Font.PLAIN, MUTED);
        y += 138;
        drawText(g, "Par\u00e7a s\u00fcresi", 72, y, 900, 40, Font.BOLD, INK);
        y += 78;
        drawRadio(g, 92, y, false, "30 saniye");
        drawRadio(g, 92, y + 96, true, "60 saniye");
        drawRadio(g, 92, y + 192, false, "90 saniye");
        drawRadio(g, 92, y + 288, false, "\u00d6zel s\u00fcre");
        drawButton(g, 72, y + 455, 936, 132, "B\u00f6l ve Kaydet", false);
        drawProgress(g, 72, y + 650, 936, 68);
        drawText(g, "aile-videosu_part_002.mp4 kaydedildi (2/3)", 72, y + 755, 936, 30, Font.PLAIN, MUTED);
        drawNavBar(g);
        g.dispose();
        return image;
    }

    private static BufferedImage drawPhoneResults() {
        BufferedImage image = phoneCanvas();
        Graphics2D g = image.createGraphics();
        quality(g);
        drawStatusBars(g);
        int y = drawAppHeader(g, 118);
        drawButton(g, 72, y + 12, 936, 132, "Video Se\u00e7", true);
        y += 190;
        drawText(g, "Tamamland\u0131. Par\u00e7alar Movies/VideoBolucu klas\u00f6r\u00fcne kaydedildi.", 72, y + 30, 900, 30, Font.PLAIN, MUTED);
        y += 150;
        drawResultsHeader(g, y, true);
        drawResultRow(g, y + 118, "aile-videosu_part_001.mp4");
        drawResultRow(g, y + 228, "aile-videosu_part_002.mp4");
        drawResultRow(g, y + 338, "aile-videosu_part_003.mp4");
        drawNavBar(g);
        g.dispose();
        return image;
    }

    private static BufferedImage phoneCanvas() {
        BufferedImage image = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(SURFACE);
        g.fillRect(0, 0, 1080, 1920);
        g.dispose();
        return image;
    }

    private static int drawAppHeader(Graphics2D g, int y) {
        drawText(g, APP_NAME, 72, y, 940, 58, Font.BOLD, INK);
        g.setColor(MUTED);
        drawWrapped(g, "Videoyu se\u00e7, par\u00e7a s\u00fcresini belirle, Movies/VideoBolucu klas\u00f6r\u00fcne kaydet ve payla\u015f.", 72, y + 82, 930, 35, Font.PLAIN, 1.15f);
        return y + 198;
    }

    private static void drawStatusBars(Graphics2D g) {
        g.setColor(SURFACE);
        g.fillRect(0, 0, 1080, 92);
        drawText(g, "19:53", 72, 56, 180, 34, Font.BOLD, new Color(66, 72, 80));
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(110, 118, 126));
        g.drawLine(850, 62, 850, 45);
        g.drawLine(870, 62, 870, 38);
        g.drawLine(890, 62, 890, 31);
        g.drawRoundRect(935, 32, 58, 34, 14, 14);
        g.fillRoundRect(940, 37, 46, 24, 10, 10);
        drawText(g, "100", 905, 58, 80, 22, Font.BOLD, new Color(66, 72, 80));
    }

    private static void drawNavBar(Graphics2D g) {
        g.setColor(new Color(180, 185, 190));
        g.fillRoundRect(345, 1858, 390, 13, 7, 7);
    }

    private static void drawButton(Graphics2D g, int x, int y, int w, int h, String text, boolean enabled) {
        Color fill = enabled ? new Color(33, 102, 165) : new Color(170, 188, 202);
        g.setColor(fill);
        g.fillRect(x, y, w, h);
        drawCentered(g, text, x, y + 3, w, h, 34, Font.PLAIN, Color.WHITE);
    }

    private static void drawRadio(Graphics2D g, int x, int y, boolean checked, String label) {
        g.setStroke(new BasicStroke(6));
        g.setColor(checked ? new Color(36, 117, 178) : new Color(103, 108, 114));
        g.drawOval(x, y, 58, 58);
        if (checked) {
            g.fillOval(x + 13, y + 13, 32, 32);
        }
        drawText(g, label, x + 80, y + 41, 760, 36, Font.PLAIN, Color.BLACK);
    }

    private static void drawInput(Graphics2D g, int x, int y, int w, String value) {
        drawText(g, value, x + 26, y + 48, w - 52, 38, Font.PLAIN, Color.BLACK);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(105, 111, 118));
        g.drawLine(x + 10, y + 70, x + w - 10, y + 70);
    }

    private static void drawProgress(Graphics2D g, int x, int y, int w, int percent) {
        g.setColor(new Color(218, 226, 232));
        g.fillRoundRect(x, y, w, 18, 9, 9);
        g.setColor(new Color(33, 102, 165));
        g.fillRoundRect(x, y, Math.round(w * percent / 100f), 18, 9, 9);
    }

    private static void drawResultsHeader(Graphics2D g, int y, boolean enabled) {
        drawText(g, "Kaydedilen par\u00e7alar", 72, y + 48, 560, 40, Font.BOLD, INK);
        Color text = enabled ? new Color(33, 102, 165) : new Color(33, 102, 165, 170);
        Color bg = enabled ? new Color(236, 243, 247) : new Color(238, 240, 240);
        g.setColor(bg);
        g.fillRoundRect(735, y, 272, 104, 10, 10);
        drawCentered(g, "T\u00fcm\u00fcn\u00fc Payla\u015f", 735, y, 272, 104, 26, Font.PLAIN, text);
    }

    private static void drawResultRow(Graphics2D g, int y, String name) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(72, y, 936, 84, 10, 10);
        g.setColor(LINE);
        g.drawRoundRect(72, y, 936, 84, 10, 10);
        drawText(g, name, 96, y + 54, 650, 26, Font.PLAIN, INK);
        g.setColor(new Color(236, 243, 247));
        g.fillRoundRect(800, y + 14, 180, 56, 10, 10);
        drawCentered(g, "Payla\u015f", 800, y + 14, 180, 56, 24, Font.PLAIN, new Color(33, 102, 165));
    }

    private static void drawWrapped(Graphics2D g, String text, int x, int y, int width, int size, int style, float lineHeight) {
        g.setFont(font(style, size));
        g.setColor(g.getColor() == null ? MUTED : g.getColor());
        FontMetrics fm = g.getFontMetrics();
        int lineY = y;
        for (String line : wrap(g, text, width)) {
            g.drawString(line, x, lineY);
            lineY += Math.round(fm.getHeight() * lineHeight);
        }
    }

    private static List<String> wrap(Graphics2D g, String text, int width) {
        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (g.getFontMetrics().stringWidth(candidate) <= width) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (current.length() > 0) {
                    lines.add(current.toString());
                }
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static void drawCentered(Graphics2D g, String text, int x, int y, int w, int h, int size, int style, Color color) {
        g.setFont(font(style, size));
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        int textX = x + (w - fm.stringWidth(text)) / 2;
        int textY = y + (h - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, textX, textY);
    }

    private static void drawText(Graphics2D g, String text, int x, int baseline, int maxWidth, int size, int style) {
        drawText(g, text, x, baseline, maxWidth, size, style, g.getColor());
    }

    private static void drawText(Graphics2D g, String text, int x, int baseline, int maxWidth, int size, int style, Color color) {
        g.setFont(font(style, size));
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        String output = text;
        while (fm.stringWidth(output) > maxWidth && output.length() > 3) {
            output = output.substring(0, output.length() - 2) + "\u2026";
        }
        g.drawString(output, x, baseline);
    }

    private static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static void save(BufferedImage image, File file) throws IOException {
        ImageIO.write(image, "png", file);
    }

    private static void mkdirs(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Directory could not be created: " + dir.getAbsolutePath());
        }
    }
}
