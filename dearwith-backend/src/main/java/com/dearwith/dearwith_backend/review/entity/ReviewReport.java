package com.dearwith.dearwith_backend.review.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseTimeEntity;
import com.dearwith.dearwith_backend.review.enums.ReviewReportReason;
import com.dearwith.dearwith_backend.review.enums.ReviewReportStatus;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReport extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", foreignKey = @ForeignKey(name="fk_report_review"))
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", foreignKey = @ForeignKey(name="fk_report_user"))
    private User user;

    @Column(length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private ReviewReportReason reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ReviewReportStatus status = ReviewReportStatus.PENDING;
}
