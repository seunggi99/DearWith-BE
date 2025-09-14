package com.dearwith.dearwith_backend.external.kakao;

import com.dearwith.dearwith_backend.external.kakao.dto.KakaoKeywordResponse;
import com.dearwith.dearwith_backend.external.kakao.dto.KakaoPlace;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
@Validated
public class PlaceSearchController {

    private final KakaoLocalService localService;

    @GetMapping("/search")
    public List<KakaoPlace> search(
            @RequestParam @NotBlank String query,
            @RequestParam(required = false) @DecimalMin(value = "-180") @DecimalMax("180") Double x,
            @RequestParam(required = false) @DecimalMin(value = "-90")  @DecimalMax("90")  Double y,
            @RequestParam(required = false) @Min(0) @Max(20000) Integer radius,
            @RequestParam(defaultValue = "1")  @Min(1)  @Max(45) Integer page,
            @RequestParam(defaultValue = "10") @Min(1)  @Max(30) Integer size
    ) {
        KakaoKeywordResponse resp = localService.searchByKeyword(query, x, y, radius, page, size);
        return resp.documents();
    }

    @GetMapping("/address")
    public List<KakaoPlace> byAddress(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "1")  @Min(1)  @Max(45) Integer page,
            @RequestParam(defaultValue = "10") @Min(1)  @Max(30) Integer size
    ) {
        return localService.searchByAddress(query, page, size).documents();
    }
}