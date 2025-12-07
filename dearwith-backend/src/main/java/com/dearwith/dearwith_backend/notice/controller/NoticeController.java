package com.dearwith.dearwith_backend.notice.controller;

import com.dearwith.dearwith_backend.event.enums.EventNoticeSort;
import com.dearwith.dearwith_backend.notice.dto.NoticeCreateRequestDto;
import com.dearwith.dearwith_backend.notice.dto.NoticeResponseDto;
import com.dearwith.dearwith_backend.notice.dto.NoticeUpdateRequestDto;
import com.dearwith.dearwith_backend.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지 목록 조회")
    @GetMapping("/notices")
    public Page<NoticeResponseDto> getNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Sort sort = Sort.by(
                Sort.Order.desc("important"),
                Sort.Order.desc("createdAt")
        );

        Pageable pageable = PageRequest.of(page, size, sort);
        return noticeService.getNotices(pageable);
    }

    @Operation(summary = "공지 상세 조회")
    @GetMapping("/notices/{id}")
    public NoticeResponseDto getNotice(@PathVariable Long id) {
        return noticeService.getNotice(id);
    }

    @Operation(summary = "공지 생성(관리자)")
    @PostMapping("/admin/notices")
    public Long createNotice(@RequestBody @Valid NoticeCreateRequestDto request) {
        return noticeService.createNotice(request);
    }

    @Operation(summary = "공지 수정(관리자)")
    @PutMapping("/admin/notices/{id}")
    public void updateNotice(
            @PathVariable Long id,
            @RequestBody @Valid NoticeUpdateRequestDto request
    ) {
        noticeService.updateNotice(id, request);
    }

    @Operation(summary = "공지 삭제(관리자)")
    @DeleteMapping("/admin/notices/{id}")
    public void deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
    }
}
