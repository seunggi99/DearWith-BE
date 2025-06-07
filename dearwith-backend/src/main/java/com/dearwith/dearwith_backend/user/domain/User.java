package com.dearwith.dearwith_backend.user.domain;

import com.dearwith.dearwith_backend.user.domain.enums.AgreementType;
import com.dearwith.dearwith_backend.user.domain.enums.Role;
import com.dearwith.dearwith_backend.user.domain.enums.UserStatus;
import com.dearwith.dearwith_backend.user.dto.AgreementStatusDto;
import jakarta.persistence.*;
import lombok.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Entity
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    private String email;
    private String password;
    private String provider; // 예: 'email', 'kakao'
    private String kakaoId; // 카카오 로그인 사용 시
    private String nickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus; // Enum (ACTIVE, SUSPENDED, DELETED)

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Agreement> agreements = new ArrayList<>();

    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
        this.updatedAt = LocalDateTime.now();
    }

    // 연관관계 편의 메서드
    public void addAgreement(Agreement agreement) {
        agreements.add(agreement);
        agreement.setUser(this);
    }

    // 동의 철회
    public void removeAgreement(Agreement agreement) {
        agreements.remove(agreement);
        agreement.setUser(null);
    }

    // 약관타입으로 동의 여부 조회
    public Optional<Agreement> findAgreement(AgreementType type) {
        return agreements.stream()
                .filter(a -> a.getType() == type)
                .findFirst();
    }

    // 특정 약관 동의/철회 갱신
    public void agreeOrUpdateAgreement(AgreementType type, boolean agreed, LocalDateTime agreedAt) {
        Optional<Agreement> existing = findAgreement(type);

        if (existing.isPresent()) {
            Agreement agreement = existing.get();
            agreement.updateAgreement(agreed, agreedAt);
        } else {
            Agreement newAgreement = Agreement.builder()
                    .type(type)
                    .agreed(agreed)
                    .agreedAt(agreedAt)
                    .build();
            this.addAgreement(newAgreement);
        }
    }

    // 모든 약관 동의 상태 조회
    public List<AgreementStatusDto> getAgreementStatuses() {
        return this.agreements.stream()
                .map(agreement -> AgreementStatusDto.builder()
                        .type(agreement.getType())
                        .agreed(agreement.isAgreed())
                        .agreedAt(agreement.getAgreedAt())
                        .build()
                )
                .collect(Collectors.toList());
    }
    public void markActive() {
        this.userStatus = UserStatus.ACTIVE;
    }
    public void markSuspended() {
        this.userStatus = UserStatus.SUSPENDED;
    }

    public void markDeleted() {
        this.userStatus = UserStatus.DELETED;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
