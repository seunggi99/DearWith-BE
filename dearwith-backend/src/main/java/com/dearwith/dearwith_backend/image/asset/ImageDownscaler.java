package com.dearwith.dearwith_backend.image.asset;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Iterator;

public final class ImageDownscaler {

    private ImageDownscaler() {}

    public static BufferedImage readWithMaxLongEdge(byte[] originalBytes, int maxLongEdge) throws Exception {
        if (originalBytes == null || originalBytes.length == 0) {
            throw new IllegalArgumentException("originalBytes is empty");
        }
        if (maxLongEdge <= 0) {
            throw new IllegalArgumentException("maxLongEdge must be positive");
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalBytes);
             ImageInputStream iis = ImageIO.createImageInputStream(bais)) {

            if (iis == null) {
                throw new IllegalStateException("ImageIO.createImageInputStream() returned null");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                BufferedImage fallback = ImageIO.read(new ByteArrayInputStream(originalBytes));
                if (fallback == null) throw new IllegalStateException("ImageIO.read() returned null");
                return maybeDownscaleByThumbnailator(fallback, maxLongEdge);
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);

                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                int longEdge = Math.max(w, h);

                ImageReadParam param = reader.getDefaultReadParam();

                // ratio = ceil(longEdge / maxLongEdge)
                int ratio = (longEdge <= maxLongEdge) ? 1 : (int) Math.ceil(longEdge / (double) maxLongEdge);
                if (ratio < 1) ratio = 1;

                if (ratio > 1) {
                    param.setSourceSubsampling(ratio, ratio, 0, 0);
                }

                BufferedImage img = reader.read(0, param);
                if (img == null) throw new IllegalStateException("reader.read() returned null");

                return maybeDownscaleByThumbnailator(img, maxLongEdge);

            } finally {
                reader.dispose();
            }
        }
    }

    private static BufferedImage maybeDownscaleByThumbnailator(BufferedImage src, int maxLongEdge) throws Exception {
        int w = src.getWidth();
        int h = src.getHeight();
        int longEdge = Math.max(w, h);

        if (longEdge <= maxLongEdge) return src;

        double scale = maxLongEdge / (double) longEdge;
        int tw = (int) Math.round(w * scale);
        int th = (int) Math.round(h * scale);

        return Thumbnails.of(src)
                .size(Math.max(1, tw), Math.max(1, th))
                .keepAspectRatio(true)
                .asBufferedImage();
    }
}