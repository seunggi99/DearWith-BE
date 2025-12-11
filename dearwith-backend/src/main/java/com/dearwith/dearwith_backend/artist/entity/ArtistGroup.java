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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistGroup extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nameKr;
    private String nameEn;

    @Column(nullable = false)
    @Builder.Default
    private Long bookmarkCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    private Image profileImage;

    private LocalDate debutDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
}
