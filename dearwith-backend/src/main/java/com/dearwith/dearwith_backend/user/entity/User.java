package com.dearwith.dearwith_backend.user.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseDeletableEntity;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.user.enums.AgreementType;
import com.dearwith.dearwith_backend.user.enums.Role;
import com.dearwith.dearwith_backend.user.enums.UserStatus;
import com.dearwith.dearwith_backend.user.dto.AgreementResponseDto;
import jakarta.persistence.*;
import lombok.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Entity
@Where(clause = "deleted_at IS NULL")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseDeletableEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(unique = true)
    private String email;
    private String password;

    @Column(unique = true)
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "profile_image_id", foreignKey = @ForeignKey(name = "fk_user_profile_image"))
    private Image profileImage;

    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus; // Enum (ACTIVE, SUSPENDED, DELETED)

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Agreement> agreements = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<SocialAccount> socialAccounts;

    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
        this.updatedAt = LocalDateTime.now();
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
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
    public void agreeOrUpdateAgreement(AgreementType type, boolean agreed) {
        Optional<Agreement> existing = findAgreement(type);

        if (existing.isPresent()) {
            Agreement agreement = existing.get();
            agreement.updateAgreement(agreed);
        } else {
            Agreement newAgreement = Agreement.builder()
                    .type(type)
                    .agreed(agreed)
                    .build();
            this.addAgreement(newAgreement);
        }
    }

    // 모든 약관 동의 상태 조회
    public List<AgreementResponseDto> getAgreementStatuses() {
        return this.agreements.stream()
                .map(agreement -> AgreementResponseDto.builder()
                        .type(agreement.getType())
                        .agreed(agreement.isAgreed())
                        .updatedAt(agreement.getUpdatedAt())
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

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }
}
