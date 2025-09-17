package com.dearwith.dearwith_backend.event.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class EventInfoDto {
    private Long id;
    private String title;
    private String imageUrl;
    private List<String> artistNamesEn;
    private List<String> artistNamesKr;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long bookmarkCount;
    private Boolean bookmarked;
}
