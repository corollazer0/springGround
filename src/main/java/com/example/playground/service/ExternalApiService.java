package com.example.playground.service;

import org.springframework.stereotype.Service;

@Service
public class ExternalApiService {
    /**
     * 외부 서버와 통신하여 데이터를 가져오는 메소드라고 가정합니다.
     * 실제 테스트에서는 이 메소드의 결과값을 가짜(Mock)로 만들어 대체하게 됩니다.
     * @return 외부에서 받아온 원본 데이터 문자열
     */
    public String fetchDataFromServer() {
        // 테스트를 위한 임시 데이터이며, 실제 프로덕션 코드에서는
        // 외부 API를 호출하는 로직이 포함될 것입니다.
        return "[abc=secret, xyz=password, test=value]";
    }
}
