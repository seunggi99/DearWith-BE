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
}
