package com.dearwith.dearwith_backend.review.entity;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.jpa.BaseDeletableEntity;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.review.enums.ReviewStatus;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Entity
@Where(clause = "deleted_at IS NULL")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseDeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_review_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", foreignKey = @ForeignKey(name = "fk_review_event"))
    private Event event;

    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer reportCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.VISIBLE;

    @ElementCollection
    @CollectionTable(
            name = "review_tag",
            joinColumns = @JoinColumn(name = "review_id",
                    foreignKey = @ForeignKey(name = "fk_review_tag_review"))
    )
    @Column(name = "tag", length = 30, nullable = false)
    @OrderColumn(name = "display_order")
    @BatchSize(size = 100)
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ReviewImageMapping> images = new ArrayList<>();


    public void addImageMapping(ReviewImageMapping mapping) {
        if (images == null) images = new ArrayList<>();
        mapping.setReview(this);
        if (this.event != null) {
            mapping.setEventId(this.event.getId());
        }
        images.add(mapping);
    }

    public void clearImages() {
        if (images == null) { images = new ArrayList<>(); return; }
        images.clear();
    }

    public void addTag(String tag) {
        if (tags == null) tags = new ArrayList<>();
        if (tags.size() >= 4) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "태그는 최대 4개까지만 등록할 수 있습니다.");
        }
        tags.add(tag);
    }

    public void incLike() { this.likeCount++; }
    public void decLike() { if (this.likeCount > 0) this.likeCount--; }

    public void incReport() { this.reportCount++; }
}
