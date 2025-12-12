package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.dto.*;
import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.entity.ArtistBookmark;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroupBookmark;
import com.dearwith.dearwith_backend.artist.repository.ArtistBookmarkRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupBookmarkRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import com.dearwith.dearwith_backend.event.enums.EventType;
import com.dearwith.dearwith_backend.event.repository.EventRepository;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dearwith.dearwith_backend.event.repository.ArtistBirthdayCafeCount;
import com.dearwith.dearwith_backend.event.repository.GroupBirthdayCafeCount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistUnifiedService {

    private final ArtistRepository artistRepository;
    private final ArtistGroupRepository artistGroupRepository;
    private final ArtistBookmarkRepository artistBookmarkRepository;
    private final ArtistGroupBookmarkRepository artistGroupBookmarkRepository;
    private final ArtistService artistService;
    private final ArtistGroupService artistGroupService;
    private final AssetUrlService assetUrlService;
    private final UserReader userReader;
    private final EventRepository eventRepository;

    /*──────────────────────────────────────────────
     | 1. 이번 달 기념일(아티스트 생일 + 그룹 데뷔일)
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "thisMonthAnniversaries",
            key = "#root.methodName + T(java.time.LocalDate).now()"
    )
    public List<MonthlyAnniversaryDto> getThisMonthArtistAndGroupAnniversaries() {

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

        // 3) 생일카페 개수 집계
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

        if (all.isEmpty()) return List.of();

        // 5) 정렬 기준
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

        // 6) 오늘 후보 북마크 Top3 무조건 포함
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

        // 7) 최종 출력 (오늘 우선)
        return picked.stream()
                .sorted(finalSorter)
                .map(c -> c.dto)
                .toList();
    }

    /*──────────────────────────────────────────────
     | 2. 아티스트 북마크 추가/해제
     *──────────────────────────────────────────────*/
    @Transactional
    public ArtistBookmarkResponseDto addArtistBookmark(Long artistId, UUID userId) {

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "아티스트를 찾을 수 없습니다"
                ));

        User user = userReader.getLoginAllowedUser(userId);

        try {
            artistBookmarkRepository.save(
                    ArtistBookmark.builder()
                            .artist(artist)
                            .user(user)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ALREADY_BOOKMARKED,
                    "이미 북마크한 아티스트입니다.",
                    "ARTIST_ALREADY_BOOKMARKED"
            );
        }

        artistRepository.incrementBookmark(artistId);
        long count = artistRepository.getBookmarkCount(artistId);

        return new ArtistBookmarkResponseDto(
                artistId,
                true,
                count
        );
    }

    @Transactional
    public ArtistBookmarkResponseDto removeArtistBookmark(Long artistId, UUID userId) {

        User user = userReader.getLoginAllowedUser(userId);

        ArtistBookmark bookmark = artistBookmarkRepository
                .findByArtistIdAndUserId(artistId, user.getId())
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.BOOKMARK_NOT_FOUND,
                        "해당 아티스트 북마크를 찾을 수 없습니다.",
                        "ARTIST_BOOKMARK_NOT_FOUND"
                ));

        artistBookmarkRepository.delete(bookmark);
        artistRepository.decrementBookmark(artistId);

        long count = artistRepository.getBookmarkCount(artistId);

        return new ArtistBookmarkResponseDto(
                artistId,
                false,
                count
        );
    }

    /*──────────────────────────────────────────────
     | 3. 아티스트 그룹 북마크 추가/해제
     *──────────────────────────────────────────────*/
    @Transactional
    public ArtistGroupBookmarkResponseDto addArtistGroupBookmark(Long groupId, UUID userId) {

        ArtistGroup group = artistGroupRepository.findById(groupId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "아티스트 그룹을 찾을 수 없습니다."
                ));

        User user = userReader.getLoginAllowedUser(userId);

        try {
            artistGroupBookmarkRepository.save(
                    ArtistGroupBookmark.builder()
                            .artistGroup(group)
                            .user(user)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ALREADY_BOOKMARKED,
                    "이미 북마크한 아티스트 그룹입니다.",
                    "ARTIST_GROUP_ALREADY_BOOKMARKED"
            );
        }

        artistGroupRepository.incrementBookmark(groupId);
        long count = artistGroupRepository.getBookmarkCount(groupId);

        return new ArtistGroupBookmarkResponseDto(
                groupId,
                true,
                count
        );
    }

    @Transactional
    public ArtistGroupBookmarkResponseDto removeArtistGroupBookmark(Long groupId, UUID userId) {

        User user = userReader.getLoginAllowedUser(userId);

        ArtistGroupBookmark bookmark = artistGroupBookmarkRepository
                .findByArtistGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.BOOKMARK_NOT_FOUND,
                        "해당 아티스트 그룹 북마크를 찾을 수 없습니다.",
                        "ARTIST_GROUP_BOOKMARK_NOT_FOUND"
                ));

        artistGroupBookmarkRepository.delete(bookmark);
        artistGroupRepository.decrementBookmark(groupId);

        long count = artistGroupRepository.getBookmarkCount(groupId);

        return new ArtistGroupBookmarkResponseDto(
                groupId,
                false,
                count
        );
    }

    /*──────────────────────────────────────────────
     | 4. 통합 북마크 목록 조회
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public Page<ArtistUnifiedResponseDto> getBookmarkedArtistsAndGroups(UUID userId, Pageable pageable) {

        User user = userReader.getLoginAllowedUser(userId);

        List<ArtistBookmark> artistBookmarks = artistBookmarkRepository.findByUserId(user.getId());
        List<ArtistGroupBookmark> groupBookmarks = artistGroupBookmarkRepository.findByUserId(user.getId());

        List<ArtistUnifiedResponseDto> merged = new ArrayList<>();

        // ArtistBookmark -> DTO
        for (ArtistBookmark bm : artistBookmarks) {
            Artist artist = bm.getArtist();
            Image profileImage = artist.getProfileImage();

            String imageUrl = assetUrlService.generatePublicUrl(profileImage);
            LocalDateTime bookmarkedAt = bm.getCreatedAt();

            merged.add(new ArtistUnifiedResponseDto(
                    artist.getId(),
                    artist.getNameKr(),
                    imageUrl,
                    ArtistUnifiedResponseDto.Type.ARTIST,
                    bookmarkedAt,
                    artist.getBirthDate(),
                    artist.getDebutDate(),
                    true
            ));
        }

        // ArtistGroupBookmark -> DTO
        for (ArtistGroupBookmark bm : groupBookmarks) {
            ArtistGroup group = bm.getArtistGroup();
            Image profileImage = group.getProfileImage();

            String imageUrl = assetUrlService.generatePublicUrl(profileImage);

            LocalDateTime bookmarkedAt = bm.getCreatedAt();

            merged.add(new ArtistUnifiedResponseDto(
                    group.getId(),
                    group.getNameKr(),
                    imageUrl,
                    ArtistUnifiedResponseDto.Type.GROUP,
                    bookmarkedAt,
                    null,
                    group.getDebutDate(),
                    true
            ));
        }

        merged.sort(
                Comparator.comparing(
                                ArtistUnifiedResponseDto::createdAt,
                                Comparator.nullsLast(LocalDateTime::compareTo)
                        )
                        .reversed()
        );

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int total = merged.size();

        int start = page * size;
        if (start >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        int end = Math.min(start + size, total);
        List<ArtistUnifiedResponseDto> pageContent = merged.subList(start, end);

        return new PageImpl<>(pageContent, pageable, total);
    }

    /*──────────────────────────────────────────────
     | 5. 통합 검색 (아티스트 + 그룹)
     *──────────────────────────────────────────────*/

    public Page<ArtistUnifiedDto> searchUnified(String query, Pageable pageable) {

        String q = (query == null) ? "" : query.trim();
        if (q.isEmpty()) {
            return Page.empty(pageable);
        }

        String qLower = q.toLowerCase();

        List<ArtistDto> artistList = artistService.searchForUnified(q);
        List<ArtistGroupDto> groupList = artistGroupService.searchForUnified(q);

        List<ArtistUnifiedDto> merged = new ArrayList<>();

        for (ArtistDto dto : artistList) {
            merged.add(new ArtistUnifiedDto(
                    dto.id(),
                    dto.nameKr(),
                    dto.imageUrl(),
                    ArtistUnifiedDto.Type.ARTIST,
                    null,
                    dto.birthDate(),
                    dto.debutDate()
            ));
        }

        for (ArtistGroupDto dto : groupList) {
            merged.add(new ArtistUnifiedDto(
                    dto.id(),
                    dto.nameKr(),
                    dto.imageUrl(),
                    ArtistUnifiedDto.Type.GROUP,
                    null,
                    null,
                    dto.debutDate()
            ));
        }

        merged.sort(
                Comparator
                        .comparingInt((ArtistUnifiedDto dto) -> relevanceScore(dto, qLower))
                        .thenComparing(ArtistUnifiedDto::nameKr,
                                Comparator.nullsLast(String::compareTo))
        );

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int total = merged.size();

        int start = page * size;
        if (start >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        int end = Math.min(start + size, total);

        return new PageImpl<>(merged.subList(start, end), pageable, total);
    }

    private int relevanceScore(ArtistUnifiedDto dto, String qLower) {
        String name = dto.nameKr();
        if (name == null || name.isBlank()) {
            return 100;
        }

        String n = name.toLowerCase();

        // 0: 완전 일치
        if (n.equals(qLower)) return 0;

        // 1: 접두 일치
        if (n.startsWith(qLower)) return 1;

        // 2: 부분 포함
        if (n.contains(qLower)) return 2;

        // 3: 나머지
        return 3;
    }
    /*──────────────────────────────────────────────
     | 6. 내가 등록한 아티스트/그룹 조회
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public Page<ArtistUnifiedDto> getMyArtists(
            UUID userId,
            int page,
            int size
    ) {
        User user = userReader.getLoginAllowedUser(userId);

        List<Artist> artists = artistRepository.findByUser_Id(user.getId());
        List<ArtistGroup> groups = artistGroupRepository.findByUser_Id(user.getId());

        List<ArtistUnifiedDto> merged = new ArrayList<>(artists.size() + groups.size());

        for (Artist artist : artists) {
            Image profileImage = artist.getProfileImage();
            String imageUrl = profileImage != null
                    ? assetUrlService.generatePublicUrl(profileImage)
                    : null;

            merged.add(new ArtistUnifiedDto(
                    artist.getId(),
                    artist.getNameKr(),
                    imageUrl,
                    ArtistUnifiedDto.Type.ARTIST,
                    artist.getCreatedAt(),
                    artist.getBirthDate(),
                    artist.getDebutDate()
            ));
        }

        for (ArtistGroup group : groups) {
            Image profileImage = group.getProfileImage();
            String imageUrl = profileImage != null
                    ? assetUrlService.generatePublicUrl(profileImage)
                    : null;

            merged.add(new ArtistUnifiedDto(
                    group.getId(),
                    group.getNameKr(),
                    imageUrl,
                    ArtistUnifiedDto.Type.GROUP,
                    group.getCreatedAt(),
                    null,
                    group.getDebutDate()
            ));
        }

        merged.sort(
                Comparator.comparing(
                                ArtistUnifiedDto::createdAt,
                                Comparator.nullsLast(LocalDateTime::compareTo)
                        )
                        .reversed()
        );

        Pageable pageable = PageRequest.of(page, size);
        int total = merged.size();
        int start = page * size;

        if (start >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        int end = Math.min(start + size, total);
        List<ArtistUnifiedDto> pageContent = merged.subList(start, end);

        return new PageImpl<>(pageContent, pageable, total);
    }
}