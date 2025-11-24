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
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ArtistUnifiedService {

    private final ArtistRepository artistRepository;
    private final ArtistGroupRepository artistGroupRepository;
    private final ArtistBookmarkRepository artistBookmarkRepository;
    private final ArtistGroupBookmarkRepository artistGroupBookmarkRepository;
    private final UserRepository userRepository;
    private final ArtistService artistService;
    private final ArtistGroupService artistGroupService;

    /*──────────────────────────────────────────────
     | 1. 이번 달 기념일(아티스트 생일 + 그룹 데뷔일)
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "thisMonthAnniversaries",
            key = "#root.methodName + T(java.time.LocalDate).now().withDayOfMonth(1)"
    )
    public List<MonthlyAnniversaryDto> getThisMonthArtistAndGroupAnniversaries() {
        LocalDate today = LocalDate.now();
        int thisMonth = today.getMonthValue();
        int thisDay = today.getDayOfMonth();

        // 1) 이번 달 생일 아티스트
        List<Artist> artists = artistRepository.findArtistsByBirthMonth(thisMonth);

        List<MonthlyAnniversaryDto> artistDtos = artists.stream()
                .filter(a -> a.getBirthDate() != null)
                .map(a -> {
                    LocalDate birthDate = a.getBirthDate();

                    boolean isToday = birthDate.getMonthValue() == thisMonth
                            && birthDate.getDayOfMonth() == thisDay;

                    Integer years = today.getYear() - birthDate.getYear();
                    if (years < 1) years = 1;

                    return new MonthlyAnniversaryDto(
                            a.getId(),
                            a.getNameKr(),
                            a.getNameEn(),
                            a.getProfileImage() != null ? a.getProfileImage().getImageUrl() : null,
                            MonthlyAnniversaryDto.Type.ARTIST,
                            birthDate,
                            isToday,
                            years
                    );
                })
                .toList();

        // 2) 이번 달 데뷔 그룹
        List<ArtistGroup> groups = artistGroupRepository.findGroupsByDebutMonth(thisMonth);

        List<MonthlyAnniversaryDto> groupDtos = groups.stream()
                .filter(g -> g.getDebutDate() != null)
                .map(g -> {
                    LocalDate debutDate = g.getDebutDate();

                    boolean isToday = debutDate.getMonthValue() == thisMonth
                            && debutDate.getDayOfMonth() == thisDay;

                    Integer years = today.getYear() - debutDate.getYear();
                    if (years < 0) years = 0;

                    return new MonthlyAnniversaryDto(
                            g.getId(),
                            g.getNameKr(),
                            g.getNameEn(),
                            g.getProfileImage() != null ? g.getProfileImage().getImageUrl() : null,
                            MonthlyAnniversaryDto.Type.GROUP,
                            debutDate,
                            isToday,
                            years
                    );
                })
                .toList();

        List<MonthlyAnniversaryDto> merged = new ArrayList<>();
        merged.addAll(artistDtos);
        merged.addAll(groupDtos);

        merged.sort(
                Comparator.comparing((MonthlyAnniversaryDto dto) -> dto.date().getDayOfMonth())
                        .thenComparing(MonthlyAnniversaryDto::nameKr, Comparator.nullsLast(String::compareTo))
        );

        return merged;
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

        try {
            artistBookmarkRepository.save(
                    ArtistBookmark.builder()
                            .artist(artist)
                            .user(user)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // 이미 북마크된 상태로 다시 요청한 경우
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

        ArtistBookmark bookmark = artistBookmarkRepository
                .findByArtistIdAndUserId(artistId, userId)
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.withMessage(
                        ErrorCode.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

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

        ArtistGroupBookmark bookmark = artistGroupBookmarkRepository
                .findByArtistGroupIdAndUserId(groupId, userId)
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
    public Page<ArtistUnifiedDto> getBookmarkedArtistsAndGroups(UUID userId, Pageable pageable) {

        List<ArtistBookmark> artistBookmarks = artistBookmarkRepository.findByUserId(userId);
        List<ArtistGroupBookmark> groupBookmarks = artistGroupBookmarkRepository.findByUserId(userId);

        List<ArtistUnifiedDto> merged = new ArrayList<>();

        // ArtistBookmark -> DTO
        for (ArtistBookmark bm : artistBookmarks) {
            Artist artist = bm.getArtist();
            Image profileImage = artist.getProfileImage();

            String imageUrl = (profileImage != null) ? profileImage.getImageUrl() : null;
            LocalDateTime bookmarkedAt = bm.getCreatedAt();

            merged.add(new ArtistUnifiedDto(
                    artist.getId(),
                    artist.getNameKr(),
                    artist.getNameEn(),
                    imageUrl,
                    ArtistUnifiedDto.Type.ARTIST,
                    bookmarkedAt
            ));
        }

        // ArtistGroupBookmark -> DTO
        for (ArtistGroupBookmark bm : groupBookmarks) {
            ArtistGroup group = bm.getArtistGroup();
            Image profileImage = group.getProfileImage();

            String imageUrl = (profileImage != null) ? profileImage.getImageUrl() : null;
            LocalDateTime bookmarkedAt = bm.getCreatedAt();

            merged.add(new ArtistUnifiedDto(
                    group.getId(),
                    group.getNameKr(),
                    group.getNameEn(),
                    imageUrl,
                    ArtistUnifiedDto.Type.GROUP,
                    bookmarkedAt
            ));
        }

        merged.sort(
                Comparator.comparing(
                                ArtistUnifiedDto::createdAt,
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
        List<ArtistUnifiedDto> pageContent = merged.subList(start, end);

        return new PageImpl<>(pageContent, pageable, total);
    }

    /*──────────────────────────────────────────────
     | 5. 통합 검색 (아티스트 + 그룹)
     *──────────────────────────────────────────────*/
    @Transactional(readOnly = true)
    public Page<ArtistUnifiedDto> searchUnified(String query, Pageable pageable) {

        Page<ArtistDto> artistPage = artistService.search(query, PageRequest.of(0, Integer.MAX_VALUE));
        Page<ArtistGroupDto> groupPage = artistGroupService.search(query, PageRequest.of(0, Integer.MAX_VALUE));

        List<ArtistUnifiedDto> merged = new ArrayList<>();

        for (ArtistDto dto : artistPage.getContent()) {
            merged.add(new ArtistUnifiedDto(
                    dto.id(),
                    dto.nameKr(),
                    dto.nameEn(),
                    dto.imageUrl(),
                    ArtistUnifiedDto.Type.ARTIST,
                    null
            ));
        }

        for (ArtistGroupDto dto : groupPage.getContent()) {
            merged.add(new ArtistUnifiedDto(
                    dto.id(),
                    dto.nameKr(),
                    dto.nameEn(),
                    dto.imageUrl(),
                    ArtistUnifiedDto.Type.GROUP,
                    null
            ));
        }

        merged.sort(
                Comparator.comparing(ArtistUnifiedDto::nameKr, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ArtistUnifiedDto::nameEn, Comparator.nullsLast(String::compareTo))
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
}