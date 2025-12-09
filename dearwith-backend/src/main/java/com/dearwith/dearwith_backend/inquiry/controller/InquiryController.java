package com.dearwith.dearwith_backend.inquiry.controller;

import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.common.dto.CreatedResponseDto;
import com.dearwith.dearwith_backend.inquiry.dto.*;
import com.dearwith.dearwith_backend.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    /** 문의 등록 */
    @Operation(summary = "문의 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedResponseDto createInquiry(
            @CurrentUser UUID userId,
            @RequestBody @Valid InquiryCreateRequestDto request
    ) {
        return inquiryService.createInquiry(userId, request);
    }

    /** 내 문의 목록 (최신순) */
    @Operation(summary = "내 문의 목록 조회 (최신순)")
    @GetMapping("/my")
    public Page<InquiryInfoDto> getMyInquiries(
            @CurrentUser UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return inquiryService.getMyInquiries(userId, pageable);
    }

    /** 내 문의 상세 */
    @Operation(summary = "내 문의 상세 조회")
    @GetMapping("/{inquiryId}")
    public InquiryResponseDto getMyInquiry(
            @CurrentUser UUID userId,
            @PathVariable Long inquiryId
    ) {
        return inquiryService.getMyInquiry(userId, inquiryId);
    }

    /** 답변 만족/아쉬워요 선택 */
    @Operation(summary = "답변 만족도 선택")
    @PostMapping("/{inquiryId}/satisfaction")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSatisfaction(
            @CurrentUser UUID userId,
            @PathVariable Long inquiryId,
            @RequestBody @Valid InquirySatisfactionRequestDto request
    ) {
        inquiryService.updateSatisfaction(userId, inquiryId, request);
    }
}