package com.dearwith.dearwith_backend.user.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseTimeEntity;
import com.dearwith.dearwith_backend.user.enums.AgreementType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 약관 동의 정보
 */
@Entity
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Agreement extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    private AgreementType type;

    @Column(nullable = false)
    private boolean agreed;

    public void updateAgreement(boolean agreed) {
        this.agreed = agreed;
    }

    public void setUser(User user) {
        this.user = user;
    }
}