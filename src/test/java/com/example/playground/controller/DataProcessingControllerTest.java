package com.example.playground.controller;

import com.example.playground.service.DataMaskingService;
import com.example.playground.service.ExternalApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DataProcessingController에 대한 통합 테스트 클래스
 */
@WebMvcTest(DataProcessingController.class) // 웹 레이어(컨트롤러)만 테스트하기 위한 어노테이션
class DataProcessingControllerTest {

    @Autowired
    private MockMvc mockMvc; // 컨트롤러 테스트를 위한 가짜 HTTP 요청 객체

    // 컨트롤러가 의존하는 서비스들을 가짜(Mock) 객체로 생성
    // 실제 로직이 수행되지 않으며, 우리가 정의하는 행동만 수행함
    @MockBean
    private ExternalApiService externalApiService;

    @MockBean
    private DataMaskingService dataMaskingService;

    @Test
    @DisplayName("외부 데이터를 가져와 성공적으로 마스킹 처리 후 응답한다")
    void getAndMaskData_Success() throws Exception {
        // given - 테스트를 위한 사전 조건 설정
        String originalData = "[abc=secret, xyz=password, test=value]";
        String maskedData = "[abc=<Masked>, xyz=<Masked>, test=value]";

        // externalApiService.fetchDataFromServer() 메소드가 호출되면, 실제 로직을 수행하는 대신
        // 미리 준비된 originalData 문자열을 반환하도록 설정
        given(externalApiService.fetchDataFromServer()).willReturn(originalData);

        // dataMaskingService.maskSensitiveData() 메소드가 originalData를 인자로 받아 호출되면,
        // 실제 마스킹 로직을 수행하는 대신 미리 준비된 maskedData 문자열을 반환하도록 설정
        given(dataMaskingService.maskSensitiveData(originalData)).willReturn(maskedData);


        // when & then - 실제 테스트 수행 및 결과 검증
        // mockMvc를 통해 /api/data 주소로 GET 요청을 보낸다.
        mockMvc.perform(get("/api/data"))
                // 예상 결과: HTTP 상태 코드는 200 (OK) 이어야 한다.
                .andExpect(status().isOk())
                // 예상 결과: 응답 본문(body)의 내용은 위에서 정의한 maskedData와 일치해야 한다.
                .andExpect(content().string(maskedData));
    }
}
