package com.dearwith.dearwith_backend.artist.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseAuditableEntity;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Where(clause = "deleted_at IS NULL")
@Getter @Setter
@BatchSize(size = 50)
@NoArgsConstructor @AllArgsConstructor @Builder
public class Artist extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nameKr;
    private String nameEn;
    private String realName;
    private String realNameKr;

    @Column(nullable = false)
    @Builder.Default
    private Long bookmarkCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    private Image profileImage;

    private LocalDate birthDate; // 아티스트 생년월일
    private LocalDate debutDate;   // 활동 시작일

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
}
