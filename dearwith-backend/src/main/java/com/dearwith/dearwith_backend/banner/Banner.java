package com.dearwith.dearwith_backend.banner;

import com.dearwith.dearwith_backend.common.jpa.BaseDeletableEntity;
import com.dearwith.dearwith_backend.image.entity.Image;
import jakarta.persistence.*;

import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner extends BaseDeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id")
    private Image image;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

}
