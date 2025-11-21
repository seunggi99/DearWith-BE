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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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

    // ======================
    // 아티스트 북마크 추가/해제
    // ======================

    @Transactional
    public ArtistBookmarkResponseDto addArtistBookmark(Long artistId, UUID userId) {

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        try {
            artistBookmarkRepository.save(
                    ArtistBookmark.builder()
                            .artist(artist)
                            .user(user)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // ErrorCode 에 별도 ARTIST 북마크 에러가 있으면 그걸 쓰고, 없으면 재사용
            throw new BusinessException(ErrorCode.ALREADY_BOOKMARKED);
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
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKMARK_NOT_FOUND));

        artistBookmarkRepository.delete(bookmark);
        artistRepository.decrementBookmark(artistId);

        long count = artistRepository.getBookmarkCount(artistId);

        return new ArtistBookmarkResponseDto(
                artistId,
                false,
                count
        );
    }

    // ======================
    // 아티스트 그룹 북마크 추가/해제
    // ======================

    @Transactional
    public ArtistGroupBookmarkResponseDto addArtistGroupBookmark(Long groupId, UUID userId) {

        ArtistGroup group = artistGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        try {
            artistGroupBookmarkRepository.save(
                    ArtistGroupBookmark.builder()
                            .artistGroup(group)
                            .user(user)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // 마찬가지로 그룹 전용 에러코드 있으면 바꿔도 됨
            throw new BusinessException(ErrorCode.ALREADY_BOOKMARKED);
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
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKMARK_NOT_FOUND));

        artistGroupBookmarkRepository.delete(bookmark);
        artistGroupRepository.decrementBookmark(groupId);

        long count = artistGroupRepository.getBookmarkCount(groupId);

        return new ArtistGroupBookmarkResponseDto(
                groupId,
                false,
                count
        );
    }

    // ======================
    // 통합 조회
    // ======================

    @Transactional(readOnly = true)
    public Page<ArtistUnifiedDto> getBookmarkedArtistsAndGroups(UUID userId, Pageable pageable) {

        // 1) 전체 북마크 목록 조회
        List<ArtistBookmark> artistBookmarks = artistBookmarkRepository.findByUserId(userId);
        List<ArtistGroupBookmark> groupBookmarks = artistGroupBookmarkRepository.findByUserId(userId);

        List<ArtistUnifiedDto> merged = new ArrayList<>();

        // 2) ArtistBookmark -> ArtistUnifiedDto
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

        // 3) ArtistGroupBookmark -> ArtistUnifiedDto
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

        // 4) 북마크 생성시간 기준 정렬
        merged.sort(
                Comparator.comparing(
                        ArtistUnifiedDto::createdAt,
                        Comparator.nullsLast(LocalDateTime::compareTo)
                ).reversed()
        );

        // 5) 수동 페이징
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

    @Transactional(readOnly = true)
    public Page<ArtistUnifiedDto> searchUnified(String query, Pageable pageable) {

        // 1) DB 페이징된 결과 가져오기
        Page<ArtistDto> artistPage = artistService.search(query, PageRequest.of(0, Integer.MAX_VALUE));
        Page<ArtistGroupDto> groupPage = artistGroupService.search(query, PageRequest.of(0, Integer.MAX_VALUE));

        List<ArtistUnifiedDto> merged = new ArrayList<>();

        // 2) Artist -> Unified DTO
        for (ArtistDto dto : artistPage.getContent()) {
            merged.add(new ArtistUnifiedDto(
                    dto.id(),
                    dto.nameKr(),
                    dto.nameEn(),
                    dto.imageUrl(),
                    ArtistUnifiedDto.Type.ARTIST,
                    null   // 검색이므로 bookmarkedAt 없음
            ));
        }

        // 3) Group -> Unified DTO
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

        // 4) 정렬 (nameKr → nameEn)
        merged.sort(
                Comparator.comparing(ArtistUnifiedDto::nameKr, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ArtistUnifiedDto::nameEn, Comparator.nullsLast(String::compareTo))
        );

        // 5) 수동 페이징
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
