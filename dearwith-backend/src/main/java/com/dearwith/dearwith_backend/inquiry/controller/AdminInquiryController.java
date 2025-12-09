package com.dearwith.dearwith_backend.inquiry.controller;

import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.inquiry.dto.AdminInquiryAnswerRequestDto;
import com.dearwith.dearwith_backend.inquiry.dto.AdminInquiryInfoDto;
import com.dearwith.dearwith_backend.inquiry.dto.AdminInquiryResponseDto;
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
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;

    /** 전체 문의 목록 */
    @Operation(summary = "전체 문의 목록 조회 (관리자용)",
            description = "answered 파라미터로 답변 여부 필터링 가능")
    @GetMapping
    public Page<AdminInquiryInfoDto> getInquiries(
            @RequestParam(required = false) Boolean answered,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return inquiryService.getInquiriesForAdmin(answered, pageable);
    }

    /** 문의 상세 (관리자용) */
    @Operation(summary = "문의 상세 조회 (관리자용)")
    @GetMapping("/{inquiryId}")
    public AdminInquiryResponseDto getInquiry(
            @PathVariable Long inquiryId
    ) {
        return inquiryService.getInquiryForAdmin(inquiryId);
    }


    /** 문의 답변 등록 */
    @Operation(summary = "문의 답변 등록 (관리자용)")
    @PostMapping("/{inquiryId}/answer")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminInquiryResponseDto answerInquiry(
            @CurrentUser UUID adminId,
            @PathVariable Long inquiryId,
            @RequestBody @Valid AdminInquiryAnswerRequestDto request
    ) {
        return inquiryService.answerInquiry(inquiryId, adminId, request);
    }
}