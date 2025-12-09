package com.dearwith.dearwith_backend.inquiry.repository;

import com.dearwith.dearwith_backend.inquiry.entity.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {

    boolean existsByInquiryId(Long inquiryId);
}