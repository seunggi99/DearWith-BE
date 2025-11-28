package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.dto.ArtistGroupDto;
import com.dearwith.dearwith_backend.artist.mapper.ArtistGroupMapper;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistGroupService {
    private final ArtistGroupRepository artistGroupRepository;
    private final ArtistGroupMapper artistGroupMapper;
    private final AssetUrlService assetUrlService;

    public Page<ArtistGroupDto> search(String query, Pageable pageable) {
        return artistGroupRepository.searchByName(query, pageable)
                .map(artistGroup -> artistGroupMapper.toDto(artistGroup, assetUrlService));
    }
}
