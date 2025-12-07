package com.dearwith.dearwith_backend.notice.repository;

import com.dearwith.dearwith_backend.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Page<Notice> findByImportantTrue(Pageable pageable);
}