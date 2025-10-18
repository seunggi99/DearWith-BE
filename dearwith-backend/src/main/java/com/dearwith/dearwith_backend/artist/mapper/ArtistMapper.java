package com.dearwith.dearwith_backend.artist.mapper;

import com.dearwith.dearwith_backend.artist.dto.ArtistDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistInfoDto;
import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.image.Image;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ArtistMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nameKr", source = "nameKr")
    @Mapping(target = "nameEn", source = "nameEn")
    @Mapping(target = "birthDate", source = "birthDate")
    @Mapping(target = "debutDate", source = "debutDate")
    @Mapping(target = "imageUrl", expression = "java(toImageUrl(artist.getProfileImage()))")
    ArtistDto toDto(Artist artist);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nameKo", source = "nameKr")
    @Mapping(target = "nameEn", source = "nameEn")
    @Mapping(target = "imageUrl", expression = "java(toImageUrl(artist.getProfileImage()))")
    @Mapping(target = "birthDate", source = "birthDate")
    @Mapping(target = "debutDate", source = "debutDate")
    ArtistInfoDto toInfoDto(Artist artist);

    List<ArtistInfoDto> toInfoDtos(List<Artist> artists);


    default String toImageUrl(Image image) {
        if (image == null) return null;
        if (image.getImageUrl() != null && !image.getImageUrl().isBlank()) {
            return image.getImageUrl();
        }
        if (image.getS3Key() != null && !image.getS3Key().isBlank()) {
            // S3 key 기반 URL 구성 (DearWith 표준)
            return "https://dearwith-prod-assets-apne2.s3.ap-northeast-2.amazonaws.com/" + image.getS3Key();
        }
        return null;
    }
    List<ArtistDto> toDtos(List<Artist> list);
}
