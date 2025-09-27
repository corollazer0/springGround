package com.example.playground.controller;

import com.example.playground.service.DataMaskingService;
import com.example.playground.service.ExternalApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DataProcessingController {

    private final ExternalApiService externalApiService;
    private final DataMaskingService dataMaskingService;

    /**
     * 외부에서 데이터를 가져와 마스킹 처리 후 응답하는 API 엔드포인트
     * @return 마스킹 처리된 데이터
     */
    @GetMapping("/api/data")
    public String getAndMaskData() {
        // 1. 외부 서비스로부터 원본 데이터를 가져옵니다.
        String originalData = externalApiService.fetchDataFromServer();

        // 2. 데이터 마스킹 서비스를 이용해 민감 정보를 마스킹합니다.
        String maskedData = dataMaskingService.maskSensitiveData(originalData);

        // 3. 마스킹된 결과를 클라이언트에게 반환합니다.
        return maskedData;
    }
}
