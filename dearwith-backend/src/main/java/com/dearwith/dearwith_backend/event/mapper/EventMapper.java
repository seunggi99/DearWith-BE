package com.dearwith.dearwith_backend.event.mapper;

import com.dearwith.dearwith_backend.event.entity.*;
import com.dearwith.dearwith_backend.event.dto.EventCreateRequestDto;
import com.dearwith.dearwith_backend.event.dto.EventResponseDto;
import com.dearwith.dearwith_backend.image.entity.Image;
import com.dearwith.dearwith_backend.user.entity.User;
import org.mapstruct.*;

import java.util.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {
    // ---- Create 요청 -> Entity ----
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "artists", ignore = true)
    @Mapping(target = "artistGroups", ignore = true)
    @Mapping(target = "benefits", ignore = true)
    @Mapping(target = "bookmarkCount", constant = "0L")
    @Mapping(target = "placeInfo", source = "req.place")
    @Mapping(target = "organizer", source = "req.organizer")
    @Mapping(target = "user", source = "userId")
    Event toEvent(UUID userId, EventCreateRequestDto req);

    // userId -> User 프록시 세팅
    default User map(UUID userId) {
        return User.builder().id(userId).build();
    }

    // PlaceDto -> PlaceInfo
    @Mapping(target = "kakaoPlaceId", source = "kakaoPlaceId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "roadAddress", source = "roadAddress")
    @Mapping(target = "jibunAddress", source = "jibunAddress")
    @Mapping(target = "lon", source = "lon")
    @Mapping(target = "lat", source = "lat")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "placeUrl", source = "placeUrl")
    PlaceInfo toPlaceInfo(EventCreateRequestDto.PlaceDto dto);

    @Mapping(target = "verified", source = "verified")
    @Mapping(target = "xHandle",  source = "XHandle")
    @Mapping(target = "xId",      source = "XId")
    @Mapping(target = "xName",    source = "XName")
    EventResponseDto.OrganizerDto toOrganizerDto(OrganizerInfo info);

    // ---- Entity -> Response ----
    @Mapping(target = "place", source = "event.placeInfo")
    @Mapping(target = "organizer", source = "event.organizer")
    @Mapping(target = "images", source = "mappings")              // List<EventImageMapping> -> List<ImageDto>
    @Mapping(target = "artists", source = "artistMappings")       // List<EventArtistMapping> -> List<ArtistDto>
    @Mapping(target = "artistGroups",  source = "artistGroupMappings")
    @Mapping(target = "benefits", source = "benefits")            // List<EventBenefit> -> List<BenefitDto>
    @Mapping(target = "bookmarked", expression = "java(bookmarked)")
    EventResponseDto toResponse(
            Event event,
            List<EventImageMapping> mappings,
            List<EventBenefit> benefits,
            List<EventArtistMapping> artistMappings,
            List<EventArtistGroupMapping> artistGroupMappings,
            boolean bookmarked
    );

    // ---- Entity -> Response ----
    @Mapping(target = "place", source = "event.placeInfo")
    @Mapping(target = "organizer", source = "event.organizer")
    @Mapping(target = "images", source = "mappings")              // List<EventImageMapping> -> List<ImageDto>
    @Mapping(target = "artists", source = "artistMappings")       // List<EventArtistMapping> -> List<ArtistDto>
    @Mapping(target = "artistGroups",  source = "artistGroupMappings")
    @Mapping(target = "benefits", source = "benefits")            // List<EventBenefit> -> List<BenefitDto>
    @Mapping(target = "bookmarked", constant = "false")
    EventResponseDto toResponse(
            Event event,
            List<EventImageMapping> mappings,
            List<EventBenefit> benefits,
            List<EventArtistMapping> artistMappings,
            List<EventArtistGroupMapping> artistGroupMappings
    );

    // PlaceInfo -> Response.PlaceDto
    @Mapping(target = "kakaoPlaceId", source = "kakaoPlaceId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "roadAddress", source = "roadAddress")
    @Mapping(target = "jibunAddress", source = "jibunAddress")
    @Mapping(target = "lon", source = "lon")
    @Mapping(target = "lat", source = "lat")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "placeUrl", source = "placeUrl")
    EventResponseDto.PlaceDto toPlaceDto(PlaceInfo info);

    // EventBenefit -> Response.BenefitDto
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "benefitType", source = "benefitType")
    @Mapping(target = "dayIndex", source = "dayIndex")
    @Mapping(target = "displayOrder", source = "displayOrder")
    EventResponseDto.BenefitDto toBenefitDto(EventBenefit b);
    List<EventResponseDto.BenefitDto> toBenefitDtos(List<EventBenefit> list);

    // ---- Artist 매핑 ----
    @Mapping(target = "id", source = "artist.id")
    @Mapping(target = "nameKr", source = "artist.nameKr")
    @Mapping(target = "nameEn", source = "artist.nameEn")
    @Mapping(target = "profileImageUrl", source = "artist.profileImage.imageUrl")
    EventResponseDto.ArtistDto toArtistDto(EventArtistMapping mapping);
    List<EventResponseDto.ArtistDto> toArtistDtos(List<EventArtistMapping> mappings);

    // ---- Group 매핑 ----
    @Mapping(target = "id",     source = "artistGroup.id")
    @Mapping(target = "nameKr", source = "artistGroup.nameKr")
    @Mapping(target = "nameEn", source = "artistGroup.nameEn")
    @Mapping(target = "profileImageUrl", source = "artistGroup.profileImage.imageUrl")
    EventResponseDto.ArtistGroupDto toGroupDto(EventArtistGroupMapping mapping);

    List<EventResponseDto.ArtistGroupDto> toGroupDtos(List<EventArtistGroupMapping> mappings);

    // ---- Image 매핑 ----
    @Mapping(target = "id",           source = "image.id")
    @Mapping(target = "imageUrl",     source = "image.imageUrl")
    @Mapping(target = "displayOrder", source = "displayOrder")
    EventResponseDto.ImageDto toImageDto(EventImageMapping mapping);
    List<EventResponseDto.ImageDto> toImageDtos(List<EventImageMapping> mappings);

    default List<String> mapImageUrls(List<EventImageMapping> mappings) {
        return mappings.stream()
                .map(EventImageMapping::getImage)
                .map(Image::getImageUrl)
                .toList();
    }
}