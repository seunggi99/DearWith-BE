package com.dearwith.dearwith_backend.inquiry.repository;

import com.dearwith.dearwith_backend.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 내 문의 목록
    Page<Inquiry> findByUserId(UUID userId, Pageable pageable);

    // 내 문의 상세
    Optional<Inquiry> findByIdAndUserId(Long id, UUID userId);

    // 관리자용 전체 목록
    Page<Inquiry> findAll(Pageable pageable);

    // 관리자용 - 답변 여부 필터
    Page<Inquiry> findByAnswered(boolean answered, Pageable pageable);
}
