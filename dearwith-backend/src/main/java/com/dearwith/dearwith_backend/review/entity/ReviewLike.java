package com.dearwith.dearwith_backend.review.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseTimeEntity;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "review_like",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_like_review_user", columnNames = {"review_id", "user_id"})
)
public class ReviewLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "review_id", foreignKey = @ForeignKey(name = "fk_like_review"))
    private Review review;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_like_user"))
    private User user;
}