package com.dearwith.dearwith_backend.user.docs;

public class UserApiDocs {
    public static final String CREATE_DESC = """
            회원 가입 요청 예시입니다.
                        {
                          "email": "test@example.com",
                           "password": "testPassword",
                           "nickname": "테스트 닉네임",
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
