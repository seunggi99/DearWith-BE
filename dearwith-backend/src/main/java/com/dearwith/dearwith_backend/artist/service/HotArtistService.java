package com.dearwith.dearwith_backend.artist.service;

import com.dearwith.dearwith_backend.artist.dto.HotArtistDtoResponseDto;
import com.dearwith.dearwith_backend.artist.entity.Artist;
import com.dearwith.dearwith_backend.artist.entity.ArtistGroup;
import com.dearwith.dearwith_backend.artist.enums.ArtistType;
import com.dearwith.dearwith_backend.artist.repository.ArtistGroupRepository;
import com.dearwith.dearwith_backend.artist.repository.ArtistRepository;
import com.dearwith.dearwith_backend.external.aws.AssetUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotArtistService {

    private static final String HOT_ARTIST_KEY_PREFIX = "hot:artist:";
    private static final String HOT_GROUP_KEY_PREFIX  = "hot:group:";
    private static final String ARTIST_VIEW_DEDUP_PREFIX = "hot:viewed:artist:";
    private static final String GROUP_VIEW_DEDUP_PREFIX  = "hot:viewed:group:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ArtistRepository artistRepository;
    private final ArtistGroupRepository artistGroupRepository;
    private final AssetUrlService assetUrlService;

    // 같은 유저가 같은 아티스트/그룹에 대해 점수 올릴 수 있는 주기
    private static final Duration VIEW_DEDUP_DURATION = Duration.ofMinutes(30);

    // 각 일자별 ZSET 키 TTL (최근 3일만 유지)
    private static final Duration HOT_KEY_TTL = Duration.ofDays(3);

    /**
     * 아티스트 이벤트 목록 페이지 진입 시 호출
     * - 동일 userId + artistId 조합에 대해 30분에 1점만 반영
     * - 점수는 오늘자 키에 누적, 키는 3일 TTL
     */
    public void recordArtistView(Long artistId, UUID userId) {
        // 비로그인 사용자는 중복 방지 없이 바로 카운트(정책에 따라 조정 가능)
        if (userId == null) {
            incrementTodayArtistScore(artistId);
            return;
        }

        String dedupKey = ARTIST_VIEW_DEDUP_PREFIX + artistId + ":" + userId;
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", VIEW_DEDUP_DURATION);

        if (Boolean.TRUE.equals(first)) {
            incrementTodayArtistScore(artistId);
        }
    }

    /**
     * 그룹 이벤트 목록 페이지 진입 시 호출
     */
    public void recordGroupView(Long groupId, UUID userId) {
        if (userId == null) {
            incrementTodayGroupScore(groupId);
            return;
        }

        String dedupKey = GROUP_VIEW_DEDUP_PREFIX + groupId + ":" + userId;
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", VIEW_DEDUP_DURATION);

        if (Boolean.TRUE.equals(first)) {
            incrementTodayGroupScore(groupId);
        }
    }

    private void incrementTodayArtistScore(Long artistId) {
        String key = getTodayArtistKey();
        redisTemplate.opsForZSet().incrementScore(key, artistId.toString(), 1D);
        redisTemplate.expire(key, HOT_KEY_TTL);
    }

    private void incrementTodayGroupScore(Long groupId) {
        String key = getTodayGroupKey();
        redisTemplate.opsForZSet().incrementScore(key, groupId.toString(), 1D);
        redisTemplate.expire(key, HOT_KEY_TTL);
    }

    private String getTodayArtistKey() {
        return HOT_ARTIST_KEY_PREFIX + LocalDate.now();
    }

    private String getTodayGroupKey() {
        return HOT_GROUP_KEY_PREFIX + LocalDate.now();
    }

    /**
     * 최근 3일(오늘 + 어제 + 그제) 누적 점수 기준 HOT 아티스트/그룹 TOP 20
     */
    public List<HotArtistDtoResponseDto> getTop20() {
        // 1) 최근 3일 키 목록
        List<String> artistKeys = buildLast3DaysKeys(HOT_ARTIST_KEY_PREFIX);
        List<String> groupKeys  = buildLast3DaysKeys(HOT_GROUP_KEY_PREFIX);

        // 2) 각 키의 점수를 ID 기준으로 합산
        Map<Long, Long> artistScoreMap = aggregateScores(artistKeys);
        Map<Long, Long> groupScoreMap  = aggregateScores(groupKeys);

        // 3) DB 조회
        Map<Long, Artist> artistMap = artistRepository.findAllById(artistScoreMap.keySet())
                .stream()
                .collect(Collectors.toMap(Artist::getId, a -> a));

        Map<Long, ArtistGroup> groupMap = artistGroupRepository.findAllById(groupScoreMap.keySet())
                .stream()
                .collect(Collectors.toMap(ArtistGroup::getId, g -> g));

        // 4) DTO 매핑
        List<HotArtistDtoResponseDto> result = new ArrayList<>();

        artistScoreMap.forEach((id, score) -> {
            Artist artist = artistMap.get(id);
            if (artist == null) return;

            String imageUrl = null;
            if (artist.getProfileImage() != null) {
                imageUrl = assetUrlService.generatePublicUrl(artist.getProfileImage());
            }

            result.add(HotArtistDtoResponseDto.builder()
                    .id(id)
                    .nameKr(artist.getNameKr())
                    .imageUrl(imageUrl)
                    .type(ArtistType.ARTIST)
                    .score(score)
                    .birthDate(artist.getBirthDate())
                    .debutDate(null)
                    .build());
        });

        groupScoreMap.forEach((id, score) -> {
            ArtistGroup group = groupMap.get(id);
            if (group == null) return;

            String imageUrl = assetUrlService.generatePublicUrl(group.getProfileImage());

            result.add(HotArtistDtoResponseDto.builder()
                    .id(id)
                    .nameKr(group.getNameKr())
                    .imageUrl(imageUrl)
                    .type(ArtistType.GROUP)
                    .score(score)
                    .birthDate(null)
                    .debutDate(group.getDebutDate())
                    .build());
        });

        // 5) 점수 기준으로 통합 정렬 후 TOP 20만 반환
        return result.stream()
                .sorted(Comparator.comparingLong(HotArtistDtoResponseDto::getScore).reversed())
                .limit(20)
                .toList();
    }

    // prefix + yyyy-MM-dd 형태의 최근 3일 키 생성
    private List<String> buildLast3DaysKeys(String prefix) {
        LocalDate today = LocalDate.now();
        return List.of(
                prefix + today,
                prefix + today.minusDays(1),
                prefix + today.minusDays(2)
        );
    }

    // 여러 ZSET의 점수를 ID 기준으로 합산
    private Map<Long, Long> aggregateScores(List<String> keys) {
        Map<Long, Long> scoreMap = new HashMap<>();

        for (String key : keys) {
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
            if (tuples == null) continue;

            for (ZSetOperations.TypedTuple<String> t : tuples) {
                Long id = Long.valueOf(t.getValue());
                long score = t.getScore() == null ? 0L : t.getScore().longValue();
                scoreMap.merge(id, score, Long::sum);
            }
        }

        return scoreMap;
    }
}
