package com.dearwith.dearwith_backend.image.asset;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public final class ImageOrientationNormalizer {

    private ImageOrientationNormalizer() {}

    public static BufferedImage normalize(byte[] originalBytes, BufferedImage src) {
        int orientation = 1;

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(originalBytes));
            ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                orientation = dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception ignored) {
        }

        return switch (orientation) {
            case 3 -> rotate(src, 180);
            case 6 -> rotate(src, 90);
            case 8 -> rotate(src, 270);
            default -> src;
        };
    }

    private static BufferedImage rotate(BufferedImage src, int degrees) {
        int w = src.getWidth();
        int h = src.getHeight();

        int newW = (degrees == 90 || degrees == 270) ? h : w;
        int newH = (degrees == 90 || degrees == 270) ? w : h;

        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2 = out.createGraphics();
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
