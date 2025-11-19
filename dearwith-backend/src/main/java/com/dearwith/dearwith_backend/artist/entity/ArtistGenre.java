package com.dearwith.dearwith_backend.artist.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseDeletableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

@Entity
@Where(clause = "deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistGenre extends BaseDeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nameKr;
    private String nameEn;
    private String description;
}
