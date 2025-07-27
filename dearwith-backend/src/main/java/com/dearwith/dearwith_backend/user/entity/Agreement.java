package com.dearwith.dearwith_backend.user.entity;

import com.dearwith.dearwith_backend.user.enums.AgreementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


/**
 * 약관 동의 정보
 */
@Entity
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Agreement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")   // FK 컬럼명
    private User user;

    @Enumerated(EnumType.STRING)
    private AgreementType type;     // 어떤 약관에 동의했는지

    private boolean agreed;
    private LocalDateTime updatedAt; // 동의/거부 시각

    public void updateAgreement(boolean agreed, LocalDateTime updatedAt) {
        this.agreed = agreed;
        this.updatedAt = updatedAt;
    }

    public void setUser(User user) {
        this.user = user;
    }
}