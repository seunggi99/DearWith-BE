package com.dearwith.dearwith_backend.banner;

import com.dearwith.dearwith_backend.common.jpa.BaseDeletableEntity;
import com.dearwith.dearwith_backend.image.entity.Image;
import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Where;

@Entity
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@BatchSize(size = 50)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner extends BaseDeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Image image;

    @Column(nullable = false)
    private Integer displayOrder;

}
