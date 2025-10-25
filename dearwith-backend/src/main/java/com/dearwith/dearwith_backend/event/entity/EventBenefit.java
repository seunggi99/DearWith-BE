package com.dearwith.dearwith_backend.event.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseTimeEntity;
import com.dearwith.dearwith_backend.event.enums.BenefitType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "event_benefit",
        indexes = {
                @Index(name = "ix_event_benefit_event", columnList = "event_id"),
                @Index(name = "ix_event_benefit_type",  columnList = "benefit_type")
        }
)
public class EventBenefit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(length = 100, nullable = false)
    private String name;

//    @Column(columnDefinition = "text")
//    private String description;

//    @Column(length = 300)
//    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false, length = 20)
    private BenefitType benefitType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "day_index")
    private Integer dayIndex;

    @Column(name = "visible_from")
    private LocalDate visibleFrom;

//    @Column(name = "limit_count")
//    private Integer limitCount;

    public void setEvent(Event event) {
        this.event = event;
    }
}
