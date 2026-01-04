package com.dearwith.dearwith_backend.image.asset;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public final class ImageOrientationNormalizer {

    private ImageOrientationNormalizer() {}

    public static BufferedImage normalize(byte[] originalBytes, BufferedImage src) {
        int orientation = readOrientationSafely(originalBytes);

        return switch (orientation) {
            case 3 -> rotate(src, 180);
            case 6 -> rotate(src, 90);
            case 8 -> rotate(src, 270);
            default -> src;
        };
    }

    private static int readOrientationSafely(byte[] originalBytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(originalBytes));
            ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    private static BufferedImage rotate(BufferedImage src, int degrees) {
        int w = src.getWidth();
        int h = src.getHeight();

        boolean swap = (degrees == 90 || degrees == 270);
        int newW = swap ? h : w;
        int newH = swap ? w : h;

        int type = src.getType();
        if (type == BufferedImage.TYPE_CUSTOM) {
            type = BufferedImage.TYPE_INT_ARGB;
        }

        BufferedImage out = new BufferedImage(newW, newH, type);
        Graphics2D g2 = out.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform at = new AffineTransform();

        switch (degrees) {
            case 90 -> {
                at.translate(newW, 0);
                at.rotate(Math.toRadians(90));
            }
            case 180 -> {
                at.translate(newW, newH);
                at.rotate(Math.toRadians(180));
            }
            case 270 -> {
                at.translate(0, newH);
                at.rotate(Math.toRadians(270));
            }
            default -> {}
        }

        g2.drawImage(src, at, null);
        g2.dispose();
        return out;
    }
}