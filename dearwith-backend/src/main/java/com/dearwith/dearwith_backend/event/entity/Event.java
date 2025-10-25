package com.dearwith.dearwith_backend.event.entity;

import com.dearwith.dearwith_backend.common.jpa.BaseDeletableEntity;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import com.dearwith.dearwith_backend.event.enums.EventType;
import com.dearwith.dearwith_backend.image.Image;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event extends BaseDeletableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // 이벤트 제목
    private String description; // 이벤트 설명

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<EventImageMapping> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_image_id")
    private Image coverImage;

    @Embedded
    private PlaceInfo placeInfo; // 장소 정보 (예: 좌표, 상세 주소 등)

    private LocalDate startDate; // 이벤트 시작 날짜
    private LocalDate endDate; // 이벤트 종료 날짜

    private Long bookmarkCount;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventArtistMapping> artists = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayIndex ASC, displayOrder ASC")
    @Builder.Default
    private List<EventBenefit> benefits = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    public void addBenefit(EventBenefit benefit) {
        this.benefits.add(benefit);
        benefit.setEvent(this);
    }
    public void addImageMapping(EventImageMapping mapping) {
        this.images.add(mapping);
        mapping.setEvent(this);
    }

    public void addArtistMapping(EventArtistMapping mapping) {
        this.artists.add(mapping);
        mapping.setEvent(this);
    }

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "verified",     column = @Column(name = "organizer_verified", nullable = false)),
            @AttributeOverride(name = "twitterHandle",column = @Column(name = "organizer_twitter_handle", length = 50)),
            @AttributeOverride(name = "twitterId",    column = @Column(name = "organizer_twitter_id", length = 32)),
            @AttributeOverride(name = "twitterName",  column = @Column(name = "organizer_twitter_name", length = 60))
    })
    @Builder.Default
    private OrganizerInfo organizer = new OrganizerInfo();

}
