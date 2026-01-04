package com.dearwith.dearwith_backend.image.asset;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public final class ImageDownscaler {

    private ImageDownscaler() {}

    /**
     * 원본 이미지를 읽되, 긴 변이 maxLongEdge를 초과하면 먼저 축소해서 반환
     */
    public static BufferedImage readWithMaxLongEdge(byte[] originalBytes, int maxLongEdge) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (src == null) {
            throw new IllegalStateException("ImageIO.read() returned null");
        }

        int w = src.getWidth();
        int h = src.getHeight();
        int longEdge = Math.max(w, h);

        if (longEdge <= maxLongEdge) {
            return src;
        }

        double scale = maxLongEdge / (double) longEdge;
        int tw = (int) Math.round(w * scale);
        int th = (int) Math.round(h * scale);

        return Thumbnails.of(src)
                .size(Math.max(1, tw), Math.max(1, th))
                .keepAspectRatio(true)
                .asBufferedImage();
    }
}
