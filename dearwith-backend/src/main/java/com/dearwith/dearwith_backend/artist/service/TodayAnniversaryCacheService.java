package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.dto.MonthlyAnniversaryCacheDto;
import com.dearwith.dearwith_backend.artist.dto.MonthlyAnniversaryDto;
import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import com.dearwith.dearwith_backend.event.enums.EventType;
import com.dearwith.dearwith_backend.event.repository.ArtistBirthdayCafeCount;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import com.dearwith.dearwith_backend.event.repository.GroupBirthdayCafeCount;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodayAnniversaryCacheService {
    private final ArtistRepository artistRepository;
    private final ArtistGroupRepository artistGroupRepository;
    private final AssetUrlService assetUrlService;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "todayAnniversaries",
            key = "'thisMonth:' + T(java.time.YearMonth).now()"
    )
    public MonthlyAnniversaryCacheDto getThisMonthAnniversaryCache() {

        LocalDate today = LocalDate.now();
        int thisMonth = today.getMonthValue();
        int thisDay = today.getDayOfMonth();

        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfNextMonth = startOfMonth.plusMonths(1);

        // 1) 이번 달 아티스트 생일
        List<Artist> birthdayArtists = artistRepository.findArtistsByBirthMonth(thisMonth);
        List<Long> birthdayArtistIds = birthdayArtists.stream()
                .filter(a -> a.getBirthDate() != null)
                .map(Artist::getId)
                .toList();

        // 2) 이번 달 그룹 데뷔
        List<ArtistGroup> debutGroups = artistGroupRepository.findGroupsByDebutMonth(thisMonth);
        List<Long> debutGroupIds = debutGroups.stream()
                .filter(g -> g.getDebutDate() != null)
                .map(ArtistGroup::getId)
                .toList();

        // 3) 생일카페 집계
        Map<Long, Long> artistCafeCount = birthdayArtistIds.isEmpty()
                ? Map.of()
                : eventRepository.countBirthdayCafesByArtistIdsAndMonth(
                birthdayArtistIds,
                List.of(EventStatus.SCHEDULED, EventStatus.IN_PROGRESS),
                EventType.BIRTHDAY_CAFE,
                startOfMonth,
                startOfNextMonth
        ).stream().collect(Collectors.toMap(
                ArtistBirthdayCafeCount::getArtistId,
                ArtistBirthdayCafeCount::getCafeCount
        ));

        Map<Long, Long> groupCafeCount = debutGroupIds.isEmpty()
                ? Map.of()
                : eventRepository.countBirthdayCafesByGroupIdsAndMonth(
                debutGroupIds,
                List.of(EventStatus.SCHEDULED, EventStatus.IN_PROGRESS),
                EventType.BIRTHDAY_CAFE,
                startOfMonth,
                startOfNextMonth
        ).stream().collect(Collectors.toMap(
                GroupBirthdayCafeCount::getGroupId,
                GroupBirthdayCafeCount::getCafeCount
        ));

        // 4) 후보 통합
        class Candidate {
            final MonthlyAnniversaryDto dto;
            final long cafeCount;
            final long bookmarkCount;

            Candidate(MonthlyAnniversaryDto dto, long cafeCount, long bookmarkCount) {
                this.dto = dto;
                this.cafeCount = cafeCount;
                this.bookmarkCount = bookmarkCount;
            }

            String key() {
                return dto.type().name() + ":" + dto.id();
            }
        }

        List<Candidate> all = new ArrayList<>();

        // 아티스트 생일
        for (Artist a : birthdayArtists) {
            LocalDate birth = a.getBirthDate();
            if (birth == null) continue;

            boolean isToday = birth.getMonthValue() == thisMonth && birth.getDayOfMonth() == thisDay;
            int years = Math.max(today.getYear() - birth.getYear(), 1);

            all.add(new Candidate(
                    new MonthlyAnniversaryDto(
                            a.getId(),
                            a.getNameKr(),
                            assetUrlService.generatePublicUrl(a.getProfileImage()),
                            MonthlyAnniversaryDto.Type.ARTIST,
                            MonthlyAnniversaryDto.DateType.BIRTH,
                            birth,
                            isToday,
                            years
                    ),
                    artistCafeCount.getOrDefault(a.getId(), 0L),
                    a.getBookmarkCount() == null ? 0L : a.getBookmarkCount()
            ));
        }

        // 그룹 데뷔
        for (ArtistGroup g : debutGroups) {
            LocalDate debut = g.getDebutDate();
            if (debut == null) continue;

            boolean isToday = debut.getMonthValue() == thisMonth && debut.getDayOfMonth() == thisDay;
            int years = Math.max(today.getYear() - debut.getYear(), 0);

            all.add(new Candidate(
                    new MonthlyAnniversaryDto(
                            g.getId(),
                            g.getNameKr(),
                            assetUrlService.generatePublicUrl(g.getProfileImage()),
                            MonthlyAnniversaryDto.Type.GROUP,
                            MonthlyAnniversaryDto.DateType.DEBUT,
                            debut,
                            isToday,
                            years
                    ),
                    groupCafeCount.getOrDefault(g.getId(), 0L),
                    g.getBookmarkCount() == null ? 0L : g.getBookmarkCount()
            ));
        }

        if (all.isEmpty()) {
            return MonthlyAnniversaryCacheDto.builder()
                    .items(List.of())
                    .build();
        }

        // 5) 정렬 기준 (네가 짠 그대로)
        Comparator<Candidate> todayTop3Sorter =
                Comparator.comparingLong((Candidate c) -> c.bookmarkCount).reversed()
                        .thenComparingInt(c -> c.dto.date().getDayOfMonth())
                        .thenComparing(c -> c.dto.nameKr(), Comparator.nullsLast(String::compareTo));

        Comparator<Candidate> finalSorter =
                Comparator.comparing((Candidate c) -> c.dto.isToday()).reversed()
                        .thenComparingLong((Candidate c) -> c.cafeCount).reversed()
                        .thenComparingLong((Candidate c) -> c.bookmarkCount).reversed()
                        .thenComparingInt(c -> c.dto.date().getDayOfMonth())
                        .thenComparing(c -> c.dto.nameKr(), Comparator.nullsLast(String::compareTo));

        Comparator<Candidate> bookmarkSorter =
                Comparator.comparingLong((Candidate c) -> c.bookmarkCount).reversed()
                        .thenComparingInt(c -> c.dto.date().getDayOfMonth())
                        .thenComparing(c -> c.dto.nameKr(), Comparator.nullsLast(String::compareTo));

        // 6) 오늘 Top3 포함 로직 (그대로)
        Set<String> included = new HashSet<>();
        List<Candidate> picked = new ArrayList<>();

        all.stream()
                .filter(c -> c.dto.isToday())
                .sorted(todayTop3Sorter)
                .limit(3)
                .forEach(c -> {
                    if (included.add(c.key())) picked.add(c);
                });

        List<Candidate> cafeOnes = all.stream().filter(c -> c.cafeCount > 0).toList();
        List<Candidate> cafeZeros = all.stream().filter(c -> c.cafeCount == 0).toList();

        if (!cafeOnes.isEmpty()) {
            cafeOnes.stream()
                    .sorted(finalSorter)
                    .forEach(c -> {
                        if (included.add(c.key())) picked.add(c);
                    });

            if (picked.size() < 10) {
                int remain = 10 - picked.size();
                cafeZeros.stream()
                        .sorted(bookmarkSorter)
                        .filter(c -> !included.contains(c.key()))
                        .limit(remain)
                        .forEach(c -> {
                            included.add(c.key());
                            picked.add(c);
                        });
            }
        } else {
            int remain = 10 - picked.size();
            if (remain > 0) {
                all.stream()
                        .sorted(bookmarkSorter)
                        .filter(c -> !included.contains(c.key()))
                        .limit(remain)
                        .forEach(c -> {
                            included.add(c.key());
                            picked.add(c);
                        });
            }
        }

        List<MonthlyAnniversaryDto> result = picked.stream()
                .sorted(finalSorter)
                .map(c -> c.dto)
                .toList();

        return MonthlyAnniversaryCacheDto.builder()
                .items(result)
                .build();
    }
}
