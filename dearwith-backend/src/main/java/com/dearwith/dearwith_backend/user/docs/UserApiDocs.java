package com.dearwith.dearwith_backend.user.docs;

public class UserApiDocs {
    public static final String CREATE_DESC = """
        회원 가입 요청 예시입니다.

        {
          "email": "test@example.com",
          "password": "testPassword",
          "nickname": "테스트 닉네임",
          "emailTicket": "ticket-123456",
          "agreements": [
            {
              "type": "AGE_OVER_14",
              "agreed": true
            },
            {
              "type": "TERMS_OF_SERVICE",
              "agreed": true
            },
            {
              "type": "PERSONAL_INFORMATION",
              "agreed": true
            },
            {
              "type": "PUSH_NOTIFICATION",
              "agreed": false
            }
          ]
        }
        """;
    public static final String AD_CREATE_DESC = """
        회원 가입 요청 예시입니다.
        role : USER/ADMIN
        {
          "email": "test@example.com",
          "password": "testPassword",
          "nickname": "테스트 닉네임",
          "role": "USER",
          "agreements": [
            {
              "type": "AGE_OVER_14",
              "agreed": true
            },
            {
              "type": "TERMS_OF_SERVICE",
              "agreed": true
            },
            {
              "type": "PERSONAL_INFORMATION",
              "agreed": true
            },
            {
              "type": "PUSH_NOTIFICATION",
              "agreed": false
            }
          ]
        }
        """;
    public static final String SOCIAL_CREATE_DESC = """
        소셜 신규 회원 가입 요청 예시입니다.

        {
          "provider": "KAKAO",
          "socialId": "1234567890123456",
          "nickname": "닉네임",
          "agreements": [
            {
              "type": "AGE_OVER_14",
              "agreed": true
            },
            {
              "type": "TERMS_OF_SERVICE",
              "agreed": true
            },
            {
              "type": "PERSONAL_INFORMATION",
              "agreed": true
            },
            {
              "type": "PUSH_NOTIFICATION",
              "agreed": false
            }
          ]
        }
        """;

    public static final String DELETE_DESC = """
        회원 탈퇴 요청 예시입니다. \n
        {
          "reason": "NO_LONGER_NEEDED",
          "detail": "디어위드를 더 이상 사용하지 않아서 탈퇴합니다."
        } \n
        reason 사유는 아래와 같습니다. \n
        NO_LONGER_NEEDED   // 디어위드 사용이 필요 없어짐 \n
        LACK_OF_INFO       // 부실한 정보가 많음 \n
        TOO_COMMERCIAL     // 상업적인 내용이 많음 \n
        OTHER              // 그 외 사유 \n
        
    """;
}
