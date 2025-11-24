package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.dto.ArtistCreateRequestDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistDto;
import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroupMapping;
import com.dearwith.dearwith_backend.artist.mapper.ArtistMapper;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupMappingRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.image.service.ImageAttachmentService;
import com.dearwith.dearwith_backend.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistMapper artistMapper;
    private final ArtistGroupRepository groupRepository;
    private final ArtistGroupMappingRepository mappingRepository;
    private final ImageAttachmentService imageAttachmentService;

    @PersistenceContext
    private EntityManager entityManager;

    /* ============================================================
       검색
       ============================================================ */
    public Page<ArtistDto> search(String query, Pageable pageable) {
        return artistRepository.searchByName(query, pageable)
                .map(artistMapper::toDto);
    }

    public List<Artist> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return artistRepository.findByIdIn(ids);
    }


    /* ============================================================
       아티스트 생성
       ============================================================ */
    @Transactional
    public ArtistDto create(UUID userId, ArtistCreateRequestDto req) {

        if (req.nameKr() == null || req.nameKr().trim().isEmpty()) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.INVALID_INPUT,
                    "한국어 이름은 필수입니다.",
                    "ARTIST_NAME_KR_EMPTY"
            );
        }

        final String nameKr = req.nameKr().trim();
        final String nameEn = nameKr;  // 기본 정책 (필요 시 변경 가능)

        // 생일 포맷 검증
        LocalDate birth = req.birthDate();

        // 중복 검사 (이름 + 생일)
        if (birth != null &&
                artistRepository.existsByNameKrOrEnIgnoreCaseAndBirthDate(nameKr, birth)) {

            throw BusinessException.withMessageAndDetail(
                    ErrorCode.ARTIST_ALREADY_EXISTS,
                    "이미 존재하는 아티스트입니다.",
                    "ARTIST_DUPLICATE:" + nameKr
            );
        }

        // 작성자 FK (프록시)
        User creatorRef;
        try {
            creatorRef = entityManager.getReference(User.class, userId);
        } catch (Exception e) {
            throw BusinessException.withMessageAndDetail(
                    ErrorCode.NOT_FOUND,
                    "사용자를 찾을 수 없습니다.",
                    "USER_NOT_FOUND:" + userId
            );
        }

        // 엔티티 생성
        Artist artist = Artist.builder()
                .nameKr(nameKr)
                .nameEn(nameEn)
                .birthDate(birth)
                .userId(creatorRef)
                .build();

        // 프로필 이미지 TMP → inline → Image 등록
        if (req.imageTmpKey() != null && !req.imageTmpKey().isBlank()) {
            if (!req.imageTmpKey().startsWith("tmp/")) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_TMP_KEY,
                        "올바르지 않은 TMP 키입니다.",
                        req.imageTmpKey()
                );
            }
            imageAttachmentService.setArtistProfileImage(artist, req.imageTmpKey(), userId);
        }

        // 저장
        artist = artistRepository.save(artist);

        // ===========================
        // 그룹 매핑 처리
        // ===========================
        ArtistGroup resolvedGroup = null;

        // 1) groupId 우선
        if (req.groupId() != null && !req.groupId().isBlank()) {
            try {
                Long gid = Long.valueOf(req.groupId().trim());
                resolvedGroup = groupRepository.findById(gid)
                        .orElseThrow(() -> BusinessException.withMessageAndDetail(
                                ErrorCode.GROUP_NOT_FOUND,
                                "해당 그룹을 찾을 수 없습니다.",
                                "GROUP_NOT_FOUND:" + gid
                        ));
            } catch (NumberFormatException e) {
                throw BusinessException.withMessageAndDetail(
                        ErrorCode.INVALID_INPUT,
                        "groupId는 숫자여야 합니다.",
                        "INVALID_GROUP_ID_FORMAT:" + req.groupId()
                );
            }
        }

        // 2) groupName 처리
        else if (req.groupName() != null && !req.groupName().isBlank()) {
            String gname = req.groupName().trim();

            resolvedGroup = groupRepository.findByNameKrIgnoreCase(gname)
                    .or(() -> groupRepository.findByNameEnIgnoreCase(gname))
                    .orElseGet(() -> groupRepository.save(
                            ArtistGroup.builder()
                                    .nameKr(gname)
                                    .nameEn(gname)
                                    .build()
                    ));
        }

        // 매핑 생성
        if (resolvedGroup != null &&
                !mappingRepository.existsByArtistIdAndGroupId(artist.getId(), resolvedGroup.getId())) {

            mappingRepository.save(
                    ArtistGroupMapping.builder()
                            .artist(artist)
                            .group(resolvedGroup)
                            .build()
            );
        }

        // 응답 DTO
        return artistMapper.toDto(artist);
    }

}