package com.dearwith.dearwith_backend.event.controller;

import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import com.dearwith.dearwith_backend.event.dto.EventCreateRequestDto;
import com.dearwith.dearwith_backend.event.dto.EventInfoDto;
import com.dearwith.dearwith_backend.event.dto.EventResponseDto;
import com.dearwith.dearwith_backend.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @Operation(summary = "북마크 해제")
    @DeleteMapping("/{eventId}/bookmark")
    public ResponseEntity<Void> removeBookmark(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        eventService.removeBookmark(eventId, principal.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "북마크 추가")
    @PostMapping("/bookmark")
    public ResponseEntity<Void> addBookmark(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        eventService.addBookmark(eventId, principal.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "북마크힌 이벤트 조회",
            description = "사용자가 북마크한 이벤트들을 조회합니다. `state` 파라미터로 필터링이 가능합니다. \n\n" +
                    "- `SCHEDULED`: 시작 전 이벤트\n" +
                    "- `IN_PROGRESS`: 진행 중 이벤트\n" +
                    "- `ENDED`: 종료 된 이벤트")
    @GetMapping("/bookmark")
    public Page<EventInfoDto> getBookmarkedEvents(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestParam String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return eventService.getBookmarkedEvents(userId, state, pageable);
    }

    @PostMapping
    @Operation(
            summary = "이벤트 등록",
            description = """
            이벤트 기본정보 + X(트위터) 계정 정보 + 장소 + 이미지 + 특전 정보를 함께 등록합니다.
            
            ## X(트위터) 계정 정보(organizer)
            - /oath2/x/authorization/twitter API로 X(트위터) 인증 후 얻은 티켓(제한 30분)을 입력합니다.
            
            ## 장소(place) 정보
            - /api/places/search?query=... API로 검색한 장소 정보를 입력합니다.
          
            ## 아티스트 정보
            - /api/artists/search?query=... API로 검색한 아티스트 Id를 입력합니다.
            
            ## 이미지 업로드 플로우
            1) 프론트가 `POST /api/uploads/presign` API로 S3 업로드용 pre-signed URL을 발급받습니다. (응답: {url, key, ttl})
            2) 프론트가 해당 URL에 `PUT`으로 원본 이미지를 업로드합니다.
            3) 본 API 호출 시, `images[].tmpKey`에 위 단계에서 받은 **S3 object key**(예: `tmp/cat.jpg`)를 전달하면 서버가 tmp → inline으로 **커밋**합니다.
            
            ## 특전(benefits) 입력 규칙
            - `benefitType`: INCLUDED(기본), LUCKY_DRAW(추첨), LIMITED(일별 선착순)
            - `LIMITED`인 경우에만 `dayIndex`가 의미 있으며, 
            - `visibleFrom = 이벤트 시작일 + (dayIndex - 1)` 로 계산되어 저장됩니다.
            - `dayIndex`가 null이면 1로 간주(= 시작일 당일부터 노출), 1 미만이면 400 에러.
            
            ## 참고 사항
            - 아티스트 ID 유효 확인, 로그인, 이미지 tmpKey 키 중복 불가, 티켓 유효 시간 30분
            """)
    public ResponseEntity<EventResponseDto> createEvent(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestBody @Valid EventCreateRequestDto request
    ) {
        EventResponseDto response = eventService.createEvent(userId, request);
        return ResponseEntity
                .created(URI.create("/api/events/" + response.id()))
                .body(response);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "이벤트 상세 조회")
    public ResponseEntity<EventResponseDto> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @GetMapping
    @Operation(summary = "이벤트 검색")
    public Page<EventInfoDto> searchEvents(
            @AuthenticationPrincipal(expression = "id") UUID userId,
            @RequestParam(name = "query") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("title").ascending());

        return eventService.search(userId,query, pageable);
    }
}


