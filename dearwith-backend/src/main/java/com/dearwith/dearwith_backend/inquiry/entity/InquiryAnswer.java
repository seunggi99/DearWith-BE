package com.dearwith.dearwith_backend.inquiry.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseAuditableEntity;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class InquiryAnswer extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false, unique = true)
    private Inquiry inquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    private User admin;

    @Column(columnDefinition = "TEXT")
    private String content;
}