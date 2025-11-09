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
import com.dearwith.dearwith_backend.image.ImageAttachmentService;
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

        // 0) 중복 검사
        if (req.birthDate() != null &&
                artistRepository.existsByNameKrOrEnIgnoreCaseAndBirthDate(nameKr, req.birthDate())) {
            throw new BusinessException(ErrorCode.ARTIST_ALREADY_EXISTS,
                    "이미 존재하는 아티스트: %s (%s)".formatted(nameKr, req.birthDate()));
        }

        // 1) 작성자 FK 프록시
        User creatorRef = entityManager.getReference(User.class, userId);

        // 2) Artist 객체 생성
        Artist artist = Artist.builder()
                .nameKr(nameKr)
                .nameEn(nameEn)
                .birthDate(req.birthDate())
                .userId(creatorRef)
                .build();

        // 3) 프로필 이미지 첨부
        if (req.imageTmpKey() != null && !req.imageTmpKey().isBlank()) {
            imageAttachmentService.setArtistProfileImage(artist, req.imageTmpKey(), userId);
        }

        // 4) 저장
        artist = artistRepository.save(artist);

        // 5) 그룹 매핑
        ArtistGroup resolvedGroup = null;
        if (req.groupId() != null && !req.groupId().isBlank()) {
            Long gid = Long.valueOf(req.groupId().trim());
            resolvedGroup = groupRepository.findById(gid)
                    .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
        } else if (req.groupName() != null && !req.groupName().isBlank()) {
            String gname = req.groupName().trim();
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

        // 6) 응답 DTO
        String imageOut = (artist.getProfileImage() != null)
                ? artist.getProfileImage().getImageUrl()
                : null;

        return new ArtistDto(
                artist.getId(), artist.getNameKr(), artist.getNameEn(),
                imageOut, artist.getBirthDate(), artist.getDebutDate()
        );
    }

}
