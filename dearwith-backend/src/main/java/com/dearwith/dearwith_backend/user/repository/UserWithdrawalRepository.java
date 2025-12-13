package com.dearwith.dearwith_backend.user.repository;

import com.dearwith.dearwith_backend.user.entity.UserWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserWithdrawalRepository extends JpaRepository<UserWithdrawal, Long> {
    boolean existsByUser_Id(UUID userId);
}
