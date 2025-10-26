package com.example.playground.service;

import com.example.playground.config.MaskingManager;
import com.example.playground.config.MaskingProperties;
import com.example.playground.config.MaskingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;

@ExtendWith(MockitoExtension.class) // Mockito 사용
class MaskingServiceTest {

    @Mock // 가짜로 만들 객체
    private MaskingManager maskingManager;

    @InjectMocks // @Mock 객체를 주입받을 테스트 대상
    private MaskingService maskingService;

    private Map<String, Object> testDataMap;

    @BeforeEach
    void setUp() {
        // 마스킹을 수행할 원본 데이터 생성
        testDataMap = new HashMap<>();
        Map<String, Object> mimeinInMst = new HashMap<>();
        Map<String, Object> mimeinIn = new HashMap<>();

        // [중요] rlno를 Long 타입으로 설정하여 Object 처리 테스트
        mimeinIn.put("rlno", 9007021234567L);
        mimeinIn.put("custNm", "홍길동");
        mimeinIn.put("addr", "서울시 강남구"); // 룰이 없는 필드

        mimeinInMst.put("MIMEIN_IN", mimeinIn);
        testDataMap.put("MIMEIN_IN_MST", mimeinInMst);
    }

    @Test
    @DisplayName("여러 마스킹 룰(rlno, custNm)이 정확히 적용되어야 한다")
    void applyMasking_withMultipleRulesAndMixedTypes() {
        // 1. 가짜 룰 준비
        String mciId = "NCDP_MIMEIN10A0";
        MaskingProperties.PathRule rule1 = new MaskingProperties.PathRule();
        rule1.setJsonPath("MIMEIN_IN_MST.MIMEIN_IN.rlno");
        rule1.setMaskingType("type_rlno");

        MaskingProperties.PathRule rule2 = new MaskingProperties.PathRule();
        rule2.setJsonPath("MIMEIN_IN_MST.MIMEIN_IN.custNm");
        rule2.setMaskingType("type_name");

        List<MaskingProperties.PathRule> rules = Arrays.asList(rule1, rule2);

        // 2. Mocking 설정: maskingManager가 이 ID로 호출되면, 위 룰을 반환하도록 설정
        when(maskingManager.getMaskingRules(mciId)).thenReturn(rules);

        // 3. 테스트 대상 메소드 실행 (원본 Map이 수정됨)
        maskingService.applyMasking(mciId, testDataMap);

        // 4. 결과 검증
        // 4-1. Map 객체에서 마스킹된 값을 직접 꺼내서 확인
        Map<String, Object> innerMap = (Map<String, Object>) ((Map<String, Object>) testDataMap.get("MIMEIN_IN_MST")).get("MIMEIN_IN");

        // 4-2. rlno (Long -> String), custNm (String -> String) 마스킹 확인
        assertThat(innerMap.get("rlno")).isEqualTo("900702*******");
        assertThat(innerMap.get("custNm")).isEqualTo("홍*동");
        // 4-3. 룰이 없는 필드는 그대로인지 확인
        assertThat(innerMap.get("addr")).isEqualTo("서울시 강남구");
    }

    @Test
    @DisplayName("적용할 룰이 없으면 원본 데이터가 변경되지 않아야 한다")
    void applyMasking_withNoRules() {
        String mciId = "NO_RULES_ID";

        // 1. Mocking 설정: 빈 리스트 반환
        when(maskingManager.getMaskingRules(mciId)).thenReturn(Collections.emptyList());

        // 2. 테스트 대상 메소드 실행
        maskingService.applyMasking(mciId, testDataMap);

        // 3. 결과 검증: 원본 데이터가 그대로인지 확인
        Map<String, Object> innerMap = (Map<String, Object>) ((Map<String, Object>) testDataMap.get("MIMEIN_IN_MST")).get("MIMEIN_IN");
        assertThat(innerMap.get("rlno")).isEqualTo(9007021234567L); // Long 타입 그대로
        assertThat(innerMap.get("custNm")).isEqualTo("홍길동");
    }
}