package com.dearwith.dearwith_backend.inquiry.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseAuditableEntity;
import com.dearwith.dearwith_backend.inquiry.enums.SatisfactionStatus;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Where(clause = "deleted_at IS NULL")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Inquiry extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    // 답변 완료 여부
    @Builder.Default
    private boolean answered = false;

    // 답변 만족 여부
    @Enumerated(EnumType.STRING)
    private SatisfactionStatus satisfactionStatus;

    @OneToOne(mappedBy = "inquiry", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private InquiryAnswer answer;

}
