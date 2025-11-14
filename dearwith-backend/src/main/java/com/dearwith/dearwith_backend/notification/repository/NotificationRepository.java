package com.dearwith.dearwith_backend.notification.repository;

import com.dearwith.dearwith_backend.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {


    // 읽지 않은 알림 개수 조회
    long countByUserIdAndReadFalse(UUID userId);

    // 알림 목록 조회 (전체)
    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    // 읽지 않은 것만
    Page<Notification> findByUserIdAndReadFalse(UUID userId, Pageable pageable);

    // 단일 조회 (권한 체크용)
    Optional<Notification> findByIdAndUserId(Long id, UUID userId);

    // 모두 읽지 않은 알림 조회
    List<Notification> findByUserIdAndReadFalse(UUID userId);

    // 읽은 알림만 삭제용
    List<Notification> findByUserIdAndReadTrue(UUID userId);

    // 전체 알림 조회
    List<Notification> findByUserId(UUID userId);

    void deleteByUserIdAndReadTrue(UUID userId);
    void deleteByUserId(UUID userId);
}
