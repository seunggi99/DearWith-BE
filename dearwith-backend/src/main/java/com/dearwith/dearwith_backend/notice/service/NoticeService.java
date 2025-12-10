package com.dearwith.dearwith_backend.notice.service;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.notice.dto.NoticeCreateRequestDto;
import com.dearwith.dearwith_backend.notice.dto.NoticeResponseDto;
import com.dearwith.dearwith_backend.notice.dto.NoticeUpdateRequestDto;
import com.dearwith.dearwith_backend.notice.entity.Notice;
import com.dearwith.dearwith_backend.notice.repository.NoticeRepository;
import com.dearwith.dearwith_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final NotificationService notificationService;

    // 공지 목록
    public Page<NoticeResponseDto> getNotices(Pageable pageable) {
        return noticeRepository.findAll(pageable)
                .map(this::toDto);
    }

    // 공지 단건 조회
    public NoticeResponseDto getNotice(Long id) {
        Notice notice = findNotice(id);
        return toDto(notice);
    }

    @Transactional
    public Long createNotice(NoticeCreateRequestDto request) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .important(request.isImportant())
                .pushEnabled(request.isPushEnabled())
                .build();

        Notice saved = noticeRepository.save(notice);

        notificationService.sendSystemNoticeToAllUsers(
                saved.getId(),
                saved.getTitle(),
                saved.getContent(),
                saved.isPushEnabled()
        );

        return saved.getId();
    }

    @Transactional
    public void updateNotice(Long id, NoticeUpdateRequestDto request) {
        Notice notice = findNotice(id);
        notice.update(request.getTitle(), request.getContent(), request.isImportant());
    }

    @Transactional
    public void deleteNotice(Long id) {
        Notice notice = findNotice(id);
        notice.softDelete();
    }

    private Notice findNotice(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> BusinessException.withMessage(ErrorCode.NOT_FOUND,"공지를 찾을 수 없습니다."));
    }

    private NoticeResponseDto toDto(Notice notice) {
        return NoticeResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .important(notice.isImportant())
                .pushEnabled(notice.isPushEnabled())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
