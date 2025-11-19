package com.dearwith.dearwith_backend.artist.mapper;

import com.dearwith.dearwith_backend.artist.dto.ArtistDto;
import com.dearwith.dearwith_backend.artist.dto.ArtistInfoDto;
import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.external.aws.AwsS3ClientAdapter;
import com.dearwith.dearwith_backend.external.aws.S3ClientAdapter;
import com.dearwith.dearwith_backend.image.entity.Image;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ArtistMapper {

    // --- Artist -> ArtistDto ---
    @Mapping(target = "imageUrl",
            expression = "java(artist.getProfileImage() != null ? artist.getProfileImage().getImageUrl() : null)")
    ArtistDto toDto(Artist artist);

    // --- Artist -> ArtistInfoDto (isBirthday) ---
    @Mapping(target = "nameKo", source = "artist.nameKr")
    @Mapping(target = "isBirthday", expression = "java(isBirthday)")
    @Mapping(target = "imageUrl",
            expression = "java(artist.getProfileImage() != null ? artist.getProfileImage().getImageUrl() : null)")
    ArtistInfoDto toInfoDtoWithBirthday(
            Artist artist,
            boolean isBirthday
    );
}
