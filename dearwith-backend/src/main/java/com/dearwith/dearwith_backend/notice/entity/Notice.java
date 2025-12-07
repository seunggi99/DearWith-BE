package com.dearwith.dearwith_backend.notice.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

@Entity
@Where(clause = "deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Notice extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String content;

    private boolean important;

    private boolean pushEnabled;

    public void update(String title, String content, boolean important) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        this.important = important;
    }
}
