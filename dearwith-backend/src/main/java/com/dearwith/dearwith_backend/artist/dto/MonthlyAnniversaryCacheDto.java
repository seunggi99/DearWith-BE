package com.dearwith.dearwith_backend.artist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyAnniversaryCacheDto {
    private List<MonthlyAnniversaryDto> items;
}
