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
}
