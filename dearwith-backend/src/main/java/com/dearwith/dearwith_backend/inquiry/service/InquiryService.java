package com.dearwith.dearwith_backend.inquiry.service;

import com.dearwith.dearwith_backend.common.dto.CreatedResponseDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.inquiry.dto.*;
import com.dearwith.dearwith_backend.inquiry.entity.Inquiry;
import com.dearwith.dearwith_backend.inquiry.entity.InquiryAnswer;
import com.dearwith.dearwith_backend.inquiry.repository.InquiryAnswerRepository;
import com.dearwith.dearwith_backend.inquiry.repository.InquiryRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.dearwith.dearwith_backend.common.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final UserRepository userRepository;

    /* ================== 유저용 ================== */

    public CreatedResponseDto createInquiry(UUID userId, InquiryCreateRequestDto request) {
        User user = userRepository.getReferenceById(userId);

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .answered(false)
                .satisfactionStatus(null)
                .build();

        Inquiry saved = inquiryRepository.save(inquiry);
        return CreatedResponseDto.builder()
                .id(saved.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<InquiryInfoDto> getMyInquiries(UUID userId, Pageable pageable) {
        return inquiryRepository.findByUserId(userId, pageable)
                .map(this::toInquiryInfoDto);
    }

    @Transactional(readOnly = true)
    public InquiryResponseDto getMyInquiry(UUID userId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findByIdAndUserId(inquiryId, userId)
                .orElseThrow(() ->
                        BusinessException.withMessageAndDetail(
                                NOT_FOUND,
                                "문의가 존재하지 않습니다.",
                                "INQUIRY_NOT_FOUND"
                        )
                );
        return toInquiryResponseDto(inquiry);
    }

    public void updateSatisfaction(UUID userId, Long inquiryId, InquirySatisfactionRequestDto request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() ->
                        BusinessException.withMessageAndDetail(
                                NOT_FOUND,
                                "문의가 존재하지 않습니다.",
                                "INQUIRY_NOT_FOUND"
                        )
                );

        // 소유자 체크
        if (!inquiry.getUser().getId().equals(userId)) {
            throw BusinessException.withMessageAndDetail(
                    FORBIDDEN,
                    "본인의 문의만 평가할 수 있습니다.",
                    "INQUIRY_ACCESS_DENIED"
            );
        }

        if (!inquiry.isAnswered() || inquiry.getAnswer() == null) {
            throw BusinessException.withMessageAndDetail(
                    INVALID_INPUT,
                    "아직 답변되지 않은 문의입니다.",
                    "INQUIRY_NOT_ANSWERED"
            );
        }

        inquiry.setSatisfactionStatus(request.satisfactionStatus());
    }

    /* ================== 관리자용 ================== */

    @Transactional(readOnly = true)
    public Page<AdminInquiryInfoDto> getInquiriesForAdmin(Boolean answered, Pageable pageable) {
        Page<Inquiry> page;
        if (answered == null) {
            page = inquiryRepository.findAll(pageable);
        } else {
            page = inquiryRepository.findByAnswered(answered, pageable);
        }
        return page.map(this::toAdminInquiryInfoDto);
    }

    @Transactional(readOnly = true)
    public AdminInquiryResponseDto getInquiryForAdmin(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() ->
                        BusinessException.withMessageAndDetail(
                                NOT_FOUND,
                                "문의가 존재하지 않습니다.",
                                "INQUIRY_NOT_FOUND"
                        )
                );
        return toAdminInquiryResponseDto(inquiry);
    }

    public AdminInquiryResponseDto answerInquiry(Long inquiryId, UUID adminId, AdminInquiryAnswerRequestDto request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() ->
                        BusinessException.withMessageAndDetail(
                                NOT_FOUND,
                                "문의가 존재하지 않습니다.",
                                "INQUIRY_NOT_FOUND"
                        )
                );

        if (inquiry.isAnswered()
                || inquiry.getAnswer() != null
                || inquiryAnswerRepository.existsByInquiryId(inquiryId)) {

            throw BusinessException.withMessageAndDetail(
                    INVALID_INPUT,
                    "이미 답변된 문의입니다.",
                    "INQUIRY_ALREADY_ANSWERED"
            );
        }

        User admin = userRepository.getReferenceById(adminId);

        InquiryAnswer answer = InquiryAnswer.builder()
                .inquiry(inquiry)
                .admin(admin)
                .content(request.content())
                .build();

        inquiry.setAnswered(true);
        inquiry.setAnswer(answer); // 양방향 연결
        inquiryAnswerRepository.save(answer);

        return toAdminInquiryResponseDto(inquiry);
    }

    /* ================== Mapper ================== */

    private InquiryInfoDto toInquiryInfoDto(Inquiry inquiry) {
        return new InquiryInfoDto(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getCreatedAt(),
                inquiry.isAnswered()
        );
    }

    private InquiryResponseDto toInquiryResponseDto(Inquiry inquiry) {
        InquiryResponseDto.AnswerDto answerDto = null;

        if (inquiry.getAnswer() != null) {
            InquiryAnswer answer = inquiry.getAnswer();
            answerDto = new InquiryResponseDto.AnswerDto(
                    answer.getContent(),
                    answer.getCreatedAt()
            );
        }

        return new InquiryResponseDto(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getCreatedAt(),
                inquiry.isAnswered(),
                inquiry.getSatisfactionStatus(),
                answerDto
        );
    }

    private AdminInquiryResponseDto toAdminInquiryResponseDto(Inquiry inquiry) {
        AdminInquiryResponseDto.AnswerDto answerDto = null;
        if (inquiry.getAnswer() != null) {
            InquiryAnswer answer = inquiry.getAnswer();
            answerDto = new AdminInquiryResponseDto.AnswerDto(
                    answer.getContent(),
                    answer.getCreatedAt(),
                    answer.getAdmin() != null ? answer.getAdmin().getNickname() : null
            );
        }

        User user = inquiry.getUser();
        AdminInquiryResponseDto.UserInfoDto userInfo = new AdminInquiryResponseDto.UserInfoDto(
                user.getId(),
                user.getNickname()
        );

        return new AdminInquiryResponseDto(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getCreatedAt(),
                inquiry.isAnswered(),
                inquiry.getSatisfactionStatus(),
                answerDto,
                userInfo
        );
    }

    private AdminInquiryInfoDto toAdminInquiryInfoDto(Inquiry inquiry) {
        User user = inquiry.getUser();

        return new AdminInquiryInfoDto(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.isAnswered(),
                user != null ? user.getNickname() : null,
                inquiry.getCreatedAt()
        );
    }
}