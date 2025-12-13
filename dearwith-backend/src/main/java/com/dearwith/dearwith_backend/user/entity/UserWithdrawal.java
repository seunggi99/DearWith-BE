package com.dearwith.dearwith_backend.user.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseAuditableEntity;
import com.dearwith.dearwith_backend.user.enums.WithdrawalReason;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "user_withdrawal",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_withdrawal_user_id", columnNames = "user_id")
)
public class UserWithdrawal extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_withdrawal_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WithdrawalReason reason;

    @Column(length = 500)
    private String detail;

    public static UserWithdrawal of(User user, WithdrawalReason reason, String detail) {
        return UserWithdrawal.builder()
                .user(user)
                .reason(reason)
                .detail(detail == null ? null : detail.trim())
                .build();
    }

}
