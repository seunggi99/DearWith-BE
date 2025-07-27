package com.dearwith.dearwith_backend.event.service;

import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.entity.EventBookmark;
import com.dearwith.dearwith_backend.event.repository.EventBookmarkRepository;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventBookmarkRepository eventBookmarkRepository;
    private final UserRepository userRepository;

    public List<EventInfoDto> getRecommendedEvents(UUID userId) {
        return eventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> EventInfoDto.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl(event.getImageUrl())
                        .artistNamesKr(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameKr())
                                        .collect(Collectors.toList())
                        )
                        .artistNamesEn(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameEn())
                                        .collect(Collectors.toList())
                        )
                        .startDate(event.getStartDate())
                        .endDate(event.getEndDate())
                        .bookmarkCount(event.getBookmarkCount())
                        .bookmarked(
                                userId != null &&
                                        eventBookmarkRepository.existsByEventIdAndUserId(event.getId(), userId)
                        )
                        .build())
                .collect(Collectors.toList());
    }

    public List<EventInfoDto> getHotEvents(UUID userId) {
        return eventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> EventInfoDto.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl(event.getImageUrl())
                        .artistNamesKr(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameKr())
                                        .collect(Collectors.toList())
                        )
                        .artistNamesEn(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameEn())
                                        .collect(Collectors.toList())
                        )
                        .startDate(event.getStartDate())
                        .endDate(event.getEndDate())
                        .bookmarkCount(event.getBookmarkCount())
                        .bookmarked(
                                userId != null &&
                                        eventBookmarkRepository.existsByEventIdAndUserId(event.getId(), userId)
                        )
                        .build())
                .collect(Collectors.toList());
    }

    public List<EventInfoDto> getNewEvents(UUID userId) {
        return eventRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(event -> EventInfoDto.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl(event.getImageUrl())
                        .artistNamesKr(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameKr())
                                        .collect(Collectors.toList())
                        )
                        .artistNamesEn(
                                event.getArtists().stream()
                                        .map(mapping -> mapping.getArtist().getNameEn())
                                        .collect(Collectors.toList())
                        )
                        .startDate(event.getStartDate())
                        .endDate(event.getEndDate())
                        .bookmarkCount(event.getBookmarkCount())
                        .bookmarked(
                                userId != null &&
                                        eventBookmarkRepository.existsByEventIdAndUserId(event.getId(), userId)
                        )
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void addBookmark(Long eventId, UUID userId) {
        // 이미 북마크했는지 체크
        if (eventBookmarkRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new IllegalStateException("이미 북마크된 이벤트입니다.");
        }

        // 이벤트, 유저 존재 확인
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("이벤트를 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        EventBookmark bookmark = EventBookmark.builder()
                .event(event)
                .user(user)
                .build();

        eventBookmarkRepository.save(bookmark);
    }

    @Transactional
    public void removeBookmark(Long eventId, UUID userId) {
        EventBookmark bookmark = eventBookmarkRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("북마크가 존재하지 않습니다."));

        eventBookmarkRepository.delete(bookmark);
    }
}
