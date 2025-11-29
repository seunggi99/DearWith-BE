package com.dearwith.dearwith_backend.event.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseTimeEntity;
import com.dearwith.dearwith_backend.image.entity.Image;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(
        name = "event_image",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"event_id", "display_order"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventImageMapping extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Image image;

    @Column(nullable = false)
    private Integer displayOrder;
}
