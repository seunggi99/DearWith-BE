package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.dto.ArtistCreateRequestDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistInfoDto;
import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroupMapping;
import com.dearwith.dearwith_backend.artist.mapper.ArtistMapper;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupMappingRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.external.aws.S3UploadService;
import com.dearwith.dearwith_backend.image.Image;
import com.dearwith.dearwith_backend.image.ImageService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistService {
    private final ArtistRepository artistRepository;
    private final ArtistMapper artistMapper;
    private final S3UploadService s3UploadService;
    private final ArtistGroupRepository groupRepository;
    private final ArtistGroupMappingRepository mappingRepository;
    private final ImageService imageService;

    @PersistenceContext
    private EntityManager entityManager;

    public List<ArtistInfoDto> getTodayBirthdayArtists() {
        LocalDate today = LocalDate.now();
        List<Artist> artists = artistRepository.findArtistsByBirthDate(today);
        return artistMapper.toInfoDtos(artists);
    }

    public List<ArtistInfoDto> getThisMonthBirthdayArtists() {
        int thisMonth = LocalDate.now().getMonthValue();

        List<Artist> artists = artistRepository.findArtistsByBirthMonth(thisMonth);
        return artistMapper.toInfoDtos(artists);
    }

    public Page<ArtistDto> search(String query, Pageable pageable) {
        return artistRepository.searchByName(query, pageable).map(artistMapper::toDto);
    }

    public List<Artist> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return artistRepository.findByIdIn(ids);
    }

    @Transactional
    public ArtistDto create(UUID userId, ArtistCreateRequestDto req) {
        final String nameKr = req.nameKr().trim();
        final String nameEn = nameKr;

        // 0) 중복 검사: (nameKr OR nameEn) + birthDate
        if (req.birthDate() != null &&
                artistRepository.existsByNameKrOrEnIgnoreCaseAndBirthDate(nameKr, req.birthDate())) {
            throw new BusinessException(ErrorCode.ARTIST_ALREADY_EXISTS,
                    "이미 존재하는 아티스트: %s (%s)".formatted(nameKr, req.birthDate()));
        }

        // 1) (선택) 이미지 처리: tmpKey가 없으면 스킵, 있으면 검증/커밋
        Image registeredImage = null;
        final String tmpKey = req.ImageTmpKey();
        if (tmpKey != null && !tmpKey.isBlank()) {
            if (!tmpKey.startsWith("tmp/")) {
                throw new BusinessException(ErrorCode.INVALID_TMP_KEY, "tmpKey=" + tmpKey);
            }

            // 1-1) S3 tmp -> inline 커밋
            final String inlineKey;
            try {
                inlineKey = s3UploadService.promoteTmpToInline(tmpKey);
            } catch (IllegalArgumentException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.contains("size")) throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE, msg);
                if (msg.contains("content type")) throw new BusinessException(ErrorCode.UNSUPPORTED_IMAGE_TYPE, msg);
                throw new BusinessException(ErrorCode.S3_COMMIT_FAILED, msg);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.S3_COMMIT_FAILED, e.getMessage());
            }

            // 1-2) 커밋된 이미지 등록 (공용 Image 테이블)
            registeredImage = imageService.registerCommittedImage(inlineKey, userId);
        }

        // 2) 작성자 FK 프록시
        User creatorRef = entityManager.getReference(User.class, userId);

        // 3) 아티스트 생성 + (있으면) 프로필 이미지 매핑
        Artist artist = Artist.builder()
                .nameKr(nameKr)
                .nameEn(nameEn)
                .birthDate(req.birthDate())
                .userId(creatorRef)
                .profileImage(registeredImage)
                .build();
        artist = artistRepository.save(artist);

        // 4) 그룹 매핑 (ID 우선, 없으면 이름으로 findOrCreate)
        ArtistGroup resolvedGroup = null;
        if (req.groupId() != null && !req.groupId().isBlank()) {
            final Long gid;
            try { gid = Long.valueOf(req.groupId().trim()); }
            catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 groupId: " + req.groupId());
            }
            resolvedGroup = groupRepository.findById(gid)
                    .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND, "그룹 없음: " + gid));
        } else if (req.groupName() != null && !req.groupName().isBlank()) {
            final String gname = req.groupName().trim();
            resolvedGroup = groupRepository.findByNameKrIgnoreCase(gname)
                    .or(() -> groupRepository.findByNameEnIgnoreCase(gname))
                    .orElseGet(() -> groupRepository.save(
                            ArtistGroup.builder().nameKr(gname).nameEn(gname).build()
                    ));
        }

        if (resolvedGroup != null &&
                !mappingRepository.existsByArtistIdAndGroupId(artist.getId(), resolvedGroup.getId())) {
            mappingRepository.save(ArtistGroupMapping.builder()
                    .artist(artist).group(resolvedGroup).build());
        }

        // 5) 응답 DTO 매핑
        String imageOut = (artist.getProfileImage() != null)
                ? artist.getProfileImage().getImageUrl()
                : null;

        return new ArtistDto(
                artist.getId(), artist.getNameKr(), artist.getNameEn(),
                imageOut, artist.getBirthDate(), artist.getDebutDate()
        );
    }

}
