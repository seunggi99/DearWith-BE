package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.dto.ArtistGroupCreateRequestDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistGroupDto;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.mapper.ArtistGroupMapper;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
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
public class ArtistGroupService {

    private final ArtistGroupRepository artistGroupRepository;
    private final ArtistGroupMapper artistGroupMapper;
    private final AssetUrlService assetUrlService;
    private final UserReader userReader;
    private final ArtistGroupImageAppService artistGroupImageService;
    private final AuthService authService;

    public Page<ArtistGroupDto> search(String query, Pageable pageable) {
        return artistGroupRepository.searchByName(query, pageable)
                .map(group -> artistGroupMapper.toDto(group, assetUrlService));
    }

    public List<ArtistGroupDto> searchForUnified(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return List.of();
        }

        List<ArtistGroup> result = artistGroupRepository.searchByNameForUnified(q);
        return result.stream()
                .map(group -> artistGroupMapper.toDto(group, assetUrlService))
                .toList();
    }

    /* ======================== 생성 ======================== */

    @Transactional
    public CreatedResponseDto create(UUID userId, ArtistGroupCreateRequestDto req) {
        User user = userReader.getActiveUser(userId);

        String nameKr = normalizeNameKrOrThrow(req.nameKr());
        String nameEn = KoreanRomanizer.toLatin(nameKr);
        LocalDate debut = req.debutDate();

        validateDuplicateOnCreate(nameKr, debut);

        ArtistGroup group = ArtistGroup.builder()
                .nameKr(nameKr)
                .nameEn(nameEn)
                .debutDate(debut)
                .user(user)
                .build();

        group = artistGroupRepository.save(group);

        handleImageOnCreate(group, req.tmpKey());

        return CreatedResponseDto.builder()
                .id(group.getId())
                .build();
    }

    /* ======================== 수정 ======================== */

    @Transactional
    public void update(UUID userId, Long groupId, ArtistGroupCreateRequestDto req) {
        ArtistGroup group = artistGroupRepository.findById(groupId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.GROUP_NOT_FOUND,
                        "해당 그룹을 찾을 수 없습니다.",
                        "GROUP_NOT_FOUND:" + groupId
                ));

        User user = userReader.getActiveUser(userId);
        // owner, requester
        authService.validateOwner(group.getUser(), user, "수정할 권한이 없습니다.");

        String nameKr = normalizeNameKrOrThrow(req.nameKr());
        String nameEn = KoreanRomanizer.toLatin(nameKr);
        LocalDate debut = req.debutDate();

        validateDuplicateOnUpdate(nameKr, debut, groupId);

        group.setNameKr(nameKr);
        group.setNameEn(nameEn);
        group.setDebutDate(debut);

        handleImageOnUpdate(group, req.tmpKey());
    }

    /* ======================== 삭제 ======================== */

    @Transactional
    public void delete(UUID userId, Long groupId) {
        ArtistGroup group = artistGroupRepository.findById(groupId)
                .orElseThrow(() -> BusinessException.withMessageAndDetail(
                        ErrorCode.GROUP_NOT_FOUND,
                        "해당 그룹을 찾을 수 없습니다.",
                        "GROUP_NOT_FOUND:" + groupId
                ));

        User user = userReader.getActiveUser(userId);
        authService.validateOwner(group.getUser(), user,"삭제할 권한이 없습니다.");

        artistGroupImageService.delete(group);
        group.softDelete();
    }

    /* ======================== 내부 헬퍼 ======================== */

    private String normalizeNameKrOrThrow(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "한국어 이름은 필수입니다.",
                    "GROUP_NAME_KR_EMPTY"
            );
        }
        return rawName.trim();
    }

    private void validateDuplicateOnCreate(String nameKr, LocalDate debut) {
        if (debut == null) return;

        if (artistGroupRepository.existsByNameKrOrEnIgnoreCaseAndDebutDate(nameKr, debut)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ARTIST_ALREADY_EXISTS,
                    "이미 존재하는 그룹입니다.",
                    "GROUP_DUPLICATE:" + nameKr
            );
        }
    }

    private void validateDuplicateOnUpdate(String nameKr, LocalDate debut, Long groupId) {
        if (debut == null) return;

        if (artistGroupRepository.existsByNameKrOrEnIgnoreCaseAndDebutDateAndIdNot(nameKr, debut, groupId)) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ARTIST_ALREADY_EXISTS,
                    "이미 존재하는 그룹입니다.",
                    "GROUP_DUPLICATE:" + nameKr
            );
        }
    }

    private void handleImageOnCreate(ArtistGroup group, String tmpKey) {
        if (tmpKey == null || tmpKey.isBlank()) {
            return;
        }
        validateTmpKeyPrefix(tmpKey);
        artistGroupImageService.create(group, tmpKey);
    }

    private void handleImageOnUpdate(ArtistGroup group, String tmpKey) {
        if (tmpKey == null) {
            // 변경 없음
            return;
        }

        if (tmpKey.isBlank()) {
            // 삭제
            artistGroupImageService.delete(group);
            return;
        }

        validateTmpKeyPrefix(tmpKey);
        artistGroupImageService.update(group, tmpKey);
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