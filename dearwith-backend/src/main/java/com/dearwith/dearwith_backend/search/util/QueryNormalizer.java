package com.dearwith.dearwith_backend.search.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
public class QueryNormalizer {
    public String normalize(String raw) {
        if (raw == null) return "";
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase();
        s = s.replaceAll("[\\p{Cntrl}]+", "").replaceAll("\\s+", " ");
        return s;
    }
}
