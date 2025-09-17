package com.dearwith.dearwith_backend.event.entity;

import com.dearwith.dearwith_backend.image.Image;
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
public class EventImageMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id")
    private Image image;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
