package com.dearwith.dearwith_backend.artist.mapper;

import com.dearwith.dearwith_backend.artist.dto.ArtistDto;
import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ArtistMapper {

    @Mapping(
            target = "imageUrl",
            expression = "java(assetUrlService.generatePublicUrl(artist.getProfileImage()))"
    )
    ArtistDto toDto(Artist artist, @Context AssetUrlService assetUrlService);
}