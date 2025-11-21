package com.dearwith.dearwith_backend.artist.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseDeletableEntity;
import com.dearwith.dearwith_backend.image.entity.Image;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Where(clause = "deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistGroup extends BaseDeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nameKr;
    private String nameEn;

    @Column(nullable = false)
    @Builder.Default
    private Long bookmarkCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "profile_image_id", foreignKey = @ForeignKey(name = "fk_artistGroup_profile_image"))
    private Image profileImage;

    private LocalDate debutDate;
}
