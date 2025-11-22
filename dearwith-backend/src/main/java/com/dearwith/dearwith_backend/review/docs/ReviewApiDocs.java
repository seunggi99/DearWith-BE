package com.dearwith.dearwith_backend.review.docs;

public class ReviewApiDocs {
    public static final String UPDATE_DESC ="""
  - content: 빈 문자열("")이면 내용 삭제, null이면 변경 없음
  - images: null=변경 없음 / []=모든 이미지 제거 / [..]=교체(하단 추가 설명)
  - tags:   null=변경 없음 / []=모든 태그 제거 / [..]=전부 교체(최종 상태로 간주)

  이미지 교체 규칙
  - 유지할 이미지의 경우 imageId 입력
  - 삭제할 이미지의 경우 미입력
  - 추가할 이미지의 경우 tmpKey 입력
  - 각 항목은 기존 이미지 재사용(imageId) 또는 신규 업로드(tmpKey) 중 하나만 입력
  - displayOrder 입력 값 대로 저장(기존 이미지도 재입력)
  """;

    public static final String REPORT_DESC ="""
                    특정 리뷰에 대해 신고를 등록합니다. \s
                    동일 사용자는 같은 리뷰를 중복 신고할 수 없습니다.
                    
                    신고 사유
                    - OFF_TOPIC : 이벤트와 관련 없는 내용 
                    - HATE : 근거 없는 비난
                    - SPAM : 상업적인 내용
                    - OTHER : 그 외 사유

                    동작 방식
                    - reason(사유) + content(내용)을 함께 제출합니다.
                    - 신고가 등록되면 해당 리뷰의 reportCount가 1 증가합니다.
                    - 누적 신고 횟수가 **5회 이상**이 되는 순간, 리뷰는 자동으로 숨김 처리됩니다.

                    자동 숨김 처리
                    - 자동 숨김이 발생하면:
                        - Review.status = HIDDEN
                        - 트리거가 된 해당 신고의 status = AUTO_HIDDEN 으로 설정됩니다.

                    응답
                    - reviewId: 신고된 리뷰 ID
                    - reportCount: 총 신고 누적 수
                    - reviewStatus: 신고 처리 후 리뷰 상태
                    - autoHidden: 자동 숨김 여부 (true/false)
                    """;

}
