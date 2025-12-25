package com.dearwith.dearwith_backend.image.asset;

import com.dearwith.dearwith_backend.image.enums.ResizeMode;
import lombok.Getter;

@Getter
public class VariantSpec {

    private final String filename;
    private final Integer maxWidth;
    private final Integer maxHeight;
    private final String format;
    private final Integer quality;
    private final ResizeMode resizeMode;

    private VariantSpec(Builder b) {
        this.filename = b.filename;
        this.maxWidth = b.maxWidth;
        this.maxHeight = b.maxHeight;
        this.format = b.format;
        this.quality = b.quality;
        this.resizeMode = b.resizeMode;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String filename;
        private Integer maxWidth;
        private Integer maxHeight;
        private String format;
        private int quality = 80;

        private ResizeMode resizeMode = ResizeMode.FIT;

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder maxWidth(Integer maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public Builder maxHeight(Integer maxHeight) {
            this.maxHeight = maxHeight;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder quality(int quality) {
            this.quality = quality;
            return this;
        }

        public Builder resizeMode(ResizeMode resizeMode) {
            this.resizeMode = resizeMode;
            return this;
        }

        public VariantSpec build() {
            return new VariantSpec(this);
        }
    }
}
