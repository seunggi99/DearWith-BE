package com.dearwith.dearwith_backend.page.main;

import com.dearwith.dearwith_backend.artist.dto.ArtistInfoDto;
import com.dearwith.dearwith_backend.artist.service.ArtistService;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MainPageService {
    private final ArtistService artistService;
    private final EventService eventService;

    public MainPageResponseDto getMainPage(UUID userId) {
        List<ArtistInfoDto> birthdayArtists = artistService.getThisMonthBirthdayArtists();
        List<EventInfoDto> recommendedEvents = eventService.getRecommendedEvents(userId);
        List<EventInfoDto> hotEvents = eventService.getHotEvents(userId);
        List<EventInfoDto> newEvents = eventService.getNewEvents(userId);

        return MainPageResponseDto.builder()
                .birthdayArtists(birthdayArtists)
                .recommendedEvents(recommendedEvents)
                .hotEvents(hotEvents)
                .newEvents(newEvents)
                .build();
    }

}
