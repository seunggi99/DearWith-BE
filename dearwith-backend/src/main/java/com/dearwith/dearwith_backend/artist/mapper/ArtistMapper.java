package com.dearwith.dearwith_backend.artist.mapper;

import com.dearwith.dearwith_backend.artist.dto.ArtistDto;
import com.dearwith.dearwith_backend.artist.entity.Artist;
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
}
