package com.dearwith.dearwith_backend.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerInfo {

    @Column(name = "organizer_verified", nullable = false)
    private boolean verified;

    @Column(name = "organizer_twitter_handle", length = 50)
    private String xHandle;

    @Column(name = "organizer_twitter_id", length = 32)
    private String xId;

    @Column(name = "organizer_twitter_name", length = 60)
    private String xName;

    public void normalize() {
        if (verified) {
            if (isBlank(xHandle) || isBlank(xId)) {
                throw new IllegalStateException("인증된 주최자는 handle과 id가 필요합니다.");
            }
        } else {
            xId = null;
            xName = null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
