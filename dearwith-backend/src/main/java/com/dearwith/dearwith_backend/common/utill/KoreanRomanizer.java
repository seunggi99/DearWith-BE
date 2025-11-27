package com.dearwith.dearwith_backend.common.utill;

import com.ibm.icu.text.Transliterator;

public class KoreanRomanizer {
    private static final Transliterator HANGUL_TO_LATIN =
            Transliterator.getInstance("Hangul-Latin");

    public static String toLatin(String korean) {
        if (korean == null) return null;
        String trimmed = korean.trim();
        if (trimmed.isEmpty()) return trimmed;

        String result = HANGUL_TO_LATIN.transliterate(trimmed);

        return result;
    }
}
