package com.dearwith.dearwith_backend.event.dto;

import com.dearwith.dearwith_backend.common.dto.ImageGroupDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
public class EventInfoDto {
    private Long id;
    private String title;
    private List<ImageGroupDto> images;
    private List<String> artistNamesEn;
    private List<String> artistNamesKr;
    private List<String> groupNamesEn;
    private List<String> groupNamesKr;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime openTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime closeTime;

    private LocalDate startDate;
    private LocalDate endDate;
    private Long bookmarkCount;
    private Boolean bookmarked;
}
