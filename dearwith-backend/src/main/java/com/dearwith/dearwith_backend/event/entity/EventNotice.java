package com.dearwith.dearwith_backend.event.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseAuditableEntity;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Where(clause = "deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class EventNotice extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    private Event event;

    @ManyToOne(fetch = LAZY)
    private User user;

    @Column(length = 50)
    private String title;

    @Column(length = 300)
    private String content;

    private long viewCount;

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
