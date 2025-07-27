package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.dto.ArtistInfoDto;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistService {
    private final ArtistRepository artistRepository;

    public List<ArtistInfoDto> getTodayBirthdayArtists() {
        LocalDate today = LocalDate.now();

        return artistRepository.findArtistsByBirthDate(today)
                .stream()
                .map(artist -> ArtistInfoDto.builder()
                        .id(artist.getId())
                        .nameKo(artist.getNameKr())
                        .nameEn(artist.getNameEn())
                        .imageUrl(artist.getImageUrl())
                        .birthDate(artist.getBirthDate())
                        .debutDate(artist.getDebutDate())
                        .build())
                .collect(Collectors.toList());
    }

    public List<ArtistInfoDto> getThisMonthBirthdayArtists() {
        int thisMonth = LocalDate.now().getMonthValue();

        return artistRepository.findArtistsByBirthMonth(thisMonth)
                .stream()
                .map(artist -> ArtistInfoDto.builder()
                        .id(artist.getId())
                        .nameKo(artist.getNameKr())
                        .nameEn(artist.getNameEn())
                        .imageUrl(artist.getImageUrl())
                        .birthDate(artist.getBirthDate())
                        .debutDate(artist.getDebutDate())
                        .build())
                .collect(Collectors.toList());
    }
}
