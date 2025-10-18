package com.dearwith.dearwith_backend.artist.entity;

import com.dearwith.dearwith_backend.image.Image;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nameKr; // 아티스트 이름
    private String nameEn;
    private String description; // 아티스트 설명

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "profile_image_id", foreignKey = @ForeignKey(name = "fk_artist_profile_image"))
    private Image profileImage;

    private LocalDate birthDate; // 아티스트 생년월일
    private LocalDate debutDate;   // 활동 시작일

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private User userId;
}
