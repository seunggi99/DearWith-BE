package com.dearwith.dearwith_backend.artist.mapper;

import com.dearwith.dearwith_backend.artist.dto.ArtistGroupDto;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ArtistGroupMapper {
    @Mapping(
            target = "imageUrl",
            expression = "java(assetUrlService.generatePublicUrl(ag.getProfileImage()))"
    )
    ArtistGroupDto toDto(ArtistGroup ag, @Context AssetUrlService assetUrlService);
}
