package com.dearwith.dearwith_backend.review.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseTimeEntity;
import com.dearwith.dearwith_backend.image.Image;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "review_image",
        uniqueConstraints = {
                @UniqueConstraint(name="uk_review_image_unique", columnNames={"review_id","image_id"}),
                @UniqueConstraint(name="uk_review_image_order", columnNames={"review_id","display_order"})
        },
        indexes = {
                @Index(name="ix_review_image_review", columnList="review_id"),
                @Index(name="ix_review_image_event", columnList="event_id") // ★ 포토모음용
        }
)
public class ReviewImageMapping extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY) @JoinColumn(name="review_id", nullable=false)
    private Review review;

    @ManyToOne(fetch = LAZY) @JoinColumn(name="image_id", nullable=false)
    private Image image;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable=false) private int displayOrder;
    @PrePersist
    @PreUpdate
    private void syncEventId() {
        if (eventId == null && review != null && review.getEvent() != null) {
            this.eventId = review.getEvent().getId();
        }
    }
}
