package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.dto.ArtistCreateRequestDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistDto;
import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.mapper.ArtistMapper;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.common.dto.CreatedResponseDto;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.utill.KoreanRomanizer;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistMapper artistMapper;
    private final AssetUrlService assetUrlService;
    private final UserReader userReader;
    private final ArtistImageAppService artistImageService;
    private final AuthService authService;

    /* ============================================================
       검색
       ============================================================ */
    public Page<ArtistDto> search(String query, Pageable pageable) {
        return artistRepository.searchByName(query, pageable)
                .map(artist -> artistMapper.toDto(artist, assetUrlService));
    }

    public List<ArtistDto> searchForUnified(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return List.of();
        }

        List<Artist> result = artistRepository.searchByNameForUnified(q);
        return result.stream()
                .map(artist -> artistMapper.toDto(artist, assetUrlService))
                .toList();
    }

    public List<Artist> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return artistRepository.findByIdIn(ids);
    }

    /* ============================================================
       아티스트 생성 (이름 + 생일 + 단일 이미지)
       ============================================================ */
    @Transactional
    public CreatedResponseDto create(UUID userId, ArtistCreateRequestDto req) {
        User user = userReader.getActiveUser(userId);

        final String nameKr = normalizeNameKrOrThrow(req.nameKr());
        final String nameEn = KoreanRomanizer.toLatin(nameKr);
        final LocalDate birth = req.birthDate();

        validateDuplicateOnCreate(nameKr, birth);

        Artist artist = Artist.builder()
                .nameKr(nameKr)
                .nameEn(nameEn)
                .birthDate(birth)
                .user(user)
                .build();

        artist = artistRepository.save(artist);

        handleImageOnCreate(artist, req.tmpKey());

        return CreatedResponseDto.builder()
                .id(artist.getId())
                .build();
    }

    /* ============================================================
       아티스트 수정 (이름 + 생일 + 단일 이미지)
       ============================================================ */
    @Transactional
    public void update(UUID userId, Long artistId, ArtistCreateRequestDto req) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.ARTIST_NOT_FOUND,
                        "해당 아티스트를 찾을 수 없습니다.",
                        "ARTIST_NOT_FOUND:" + artistId
                ));

        User user = userReader.getActiveUser(userId);
        // owner, requester
        authService.validateOwner(artist.getUser(), user,"수정할 권한이 없습니다.");

        final String nameKr = normalizeNameKrOrThrow(req.nameKr());
        final String nameEn = KoreanRomanizer.toLatin(nameKr);
        final LocalDate birth = req.birthDate();

        validateDuplicateOnUpdate(nameKr, birth, artistId);

        artist.setNameKr(nameKr);
        artist.setNameEn(nameEn);
        artist.setBirthDate(birth);

        handleImageOnUpdate(artist, req.tmpKey());
    }

    /* ============================================================
       아티스트 삭제
       ============================================================ */
    @Transactional
    public void delete(UUID userId, Long artistId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.ARTIST_NOT_FOUND,
                        "해당 아티스트를 찾을 수 없습니다.",
                        "ARTIST_NOT_FOUND:" + artistId
                ));

        User user = userReader.getActiveUser(userId);
        authService.validateOwner(artist.getUser(), user,"삭제할 권한이 없습니다.");

        artistImageService.delete(artist);
        artist.softDelete();
    }

    /* ============================================================
       내부 헬퍼 메서드
       ============================================================ */

    private String normalizeNameKrOrThrow(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "한국어 이름은 필수입니다.",
                    "ARTIST_NAME_KR_EMPTY"
            );
        }
        return rawName.trim();
    }

    private void validateDuplicateOnCreate(String nameKr, LocalDate birth) {
        if (birth == null) return;

        if (artistRepository.existsByNameKrOrEnIgnoreCaseAndBirthDate(nameKr, birth)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ARTIST_ALREADY_EXISTS,
                    "이미 존재하는 아티스트입니다.",
                    "ARTIST_DUPLICATE:" + nameKr
            );
        }
    }

    private void validateDuplicateOnUpdate(String nameKr, LocalDate birth, Long artistId) {
        if (birth == null) return;

        if (artistRepository.existsByNameKrOrEnIgnoreCaseAndBirthDateAndIdNot(nameKr, birth, artistId)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ARTIST_ALREADY_EXISTS,
                    "이미 존재하는 아티스트입니다.",
                    "ARTIST_DUPLICATE:" + nameKr
            );
        }
    }

    // 생성 시 이미지 처리
    private void handleImageOnCreate(Artist artist, String tmpKey) {
        if (tmpKey == null || tmpKey.isBlank()) {
            return;
        }
        validateTmpKeyPrefix(tmpKey);

        artistImageService.create(artist, tmpKey);
    }

    // 수정 시 이미지 처리
    private void handleImageOnUpdate(Artist artist, String tmpKey) {
        // tmpKey == null → 변경 없음
        if (tmpKey == null) {
            return;
        }

        // tmpKey == "" → 삭제
        if (tmpKey.isBlank()) {
            artistImageService.delete(artist);
            return;
        }

        // 새 TMP 이미지로 교체
        validateTmpKeyPrefix(tmpKey);
        artistImageService.update(artist, tmpKey);
    }

    private void validateTmpKeyPrefix(String tmpKey) {
        if (!tmpKey.startsWith("tmp/")) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_TMP_KEY,
                    "이미지 등록 중 오류가 발생했습니다.",
                    tmpKey
            );
        }
    }
}