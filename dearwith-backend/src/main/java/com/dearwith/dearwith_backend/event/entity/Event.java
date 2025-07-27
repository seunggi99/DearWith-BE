package com.dearwith.dearwith_backend.event.entity;

import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import com.dearwith.dearwith_backend.event.enums.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // 이벤트 제목
    private String description; // 이벤트 설명
    private String imageUrl; // 이벤트 이미지 URL
    private String address; // 이벤트 장소
    private LocalDateTime startDate; // 이벤트 시작 날짜
    private LocalDateTime endDate; // 이벤트 종료 날짜
    private Long bookmarkCount;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventArtistMapping> artists = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

}
