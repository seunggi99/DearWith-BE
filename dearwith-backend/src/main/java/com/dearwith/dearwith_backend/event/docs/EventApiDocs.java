package com.dearwith.dearwith_backend.event.docs;

public class EventApiDocs {
    public static final String CREATE_DESC = """
            이벤트 기본정보 + X(트위터) 계정 정보 + 장소 + 이미지 + 특전 정보를 함께 등록합니다.
            
            ■  X(트위터) 계정 정보(organizer)
            - /oath2/x/authorization/twitter API로 X(트위터) 인증 후 얻은 티켓(제한 30분)을 입력합니다.
            
            ■  장소(place) 정보
            - /api/places/search?query=... API로 검색한 장소 정보를 입력합니다.
          
            ■  아티스트 정보
            - /api/artists/search?query=... API로 검색한 아티스트 Id를 입력합니다.
            
            ■  이미지 업로드 플로우
            1) 프론트가 `POST /api/uploads/presign` API로 S3 업로드용 pre-signed URL을 발급받습니다. (응답: {url, key, ttl})
            2) 프론트가 해당 URL에 `PUT`으로 원본 이미지를 업로드합니다.
            3) 본 API 호출 시, `images[].tmpKey`에 위 단계에서 받은 **S3 object key**(예: `tmp/cat.jpg`)를 전달하면 서버가 tmp → inline으로 **커밋**합니다.
            
            ■  특전(benefits) 입력 규칙
            - `benefitType`: INCLUDED(기본), LUCKY_DRAW(추첨), LIMITED(일별 선착순)
            - `LIMITED`인 경우에만 `dayIndex`가 의미 있으며, 
            - `visibleFrom = 이벤트 시작일 + (dayIndex - 1)` 로 계산되어 저장됩니다.
            - `dayIndex`가 null이면 1로 간주(= 시작일 당일부터 노출), 1 미만이면 400 에러.
            
            ■  참고 사항
            - 아티스트 ID 유효 확인, 로그인, 이미지 tmpKey 키 중복 불가, 티켓 유효 시간 30분
    """;

    public static final String UPDATE_DESC = """
        이벤트 정보를 수정합니다.

        [수정 규칙 안내]

        ■ 단일 필드(title, openTime, closeTime, startDate, endDate)
        - null → 해당 필드는 변경하지 않습니다 (기존 값 유지)
        - 값 존재 → 해당 필드만 덮어쓰기

        ■ 장소(place)
        - null  → 장소 정보 유지
        - 존재 → 전달된 place 정보를 기준으로 장소 전체 업데이트

        ■ 아티스트 목록(artistIds) / 아티스트 그룹 목록(artistGroupIds)
        - null → 기존 매핑 유지
        - []   → 기존 매핑 전체 삭제
        - [1, 2, 3] → 기존 매핑 전체 삭제 후 전달된 목록으로 재구성

        ■ 특전(benefits)
        - null → 기존 특전 유지
        - []   → 기존 특전 전체 삭제
        - [ ... ] → 기존 특전 전체 삭제 후 전달된 목록으로 재구성

        ■ 이미지(images)
        - 최대 10개까지 가능
        - null → 기존 이미지 유지
        - []   → 기존 이미지 전체 삭제
        - [
            { "id": 기존 이미지 ID, "displayOrder": 0 },
            { "tmpKey": 신규 업로드 키, "displayOrder": 1 }
          ]
          → id : 유지할 기존 이미지, tmpKey : 신규 업로드 이미지 tmp
          → 유지할 이미지 제외 기존 이미지 전체 삭제 후 id/tmpKey 기반으로 신규 구성

        ※ id 와 tmpKey 는 동시에 올 수 없습니다.
        ※ displayOrder 는 중복될 수 없습니다.

        ■ 수정 불가 항목
        - organizer (X 인증 정보)
        - 이벤트 생성자(user)
        - 이벤트 ID

        [요청 예시]
        {
          "title": "수정된 타이틀",
          "startDate": "2025-02-20",
          "endDate": "2025-02-22",
          "place": {
            "kakaoPlaceId": "12345",
            "name": "디어위드 카페",
            "roadAddress": "서울 강남구 ...",
            "jibunAddress": "서울 강남구 ...",
            "lon": 127.12,
            "lat": 37.55,
            "phone": "010-1234-5678",
            "placeUrl": "https://..."
          },
          "artistIds": [1, 2],
          "artistGroupIds": [],
          "benefits": [
            {
              "name": "포토카드 증정",
              "benefitType": "LIMITED",
              "dayIndex": 1,
              "displayOrder": 0
            }
          ],
          "images": [
            { "id": 10, "displayOrder": 0 },
            { "tmpKey": "tmp/abc123.jpg", "displayOrder": 1 }
          ]
        }
        """;
}
