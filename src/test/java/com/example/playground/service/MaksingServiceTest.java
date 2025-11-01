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

import java.util.*;

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

    @Test
    @DisplayName("List 타입 내부의 Map에 대해 마스킹이 적용되어야 한다")
    void applyMasking_withListType() {
        // 1. List를 포함한 테스트 데이터 생성
        Map<String, Object> testData = new HashMap<>();
        Map<String, Object> cusaftIn = new HashMap<>();

        // CUSAFT_IN_SUB를 List로 구성
        List<Map<String, Object>> cusaftInSubList = Arrays.asList(
            new HashMap<String, Object>() {{
                put("acno", "1234567890");
                put("name", "김철수");
            }},
            new HashMap<String, Object>() {{
                put("acno", "0987654321");
                put("name", "이영희");
            }}
        );

        cusaftIn.put("CUSAFT_IN_SUB", cusaftInSubList);
        testData.put("CUSAFT_IN", cusaftIn);

        // 2. 마스킹 룰 준비
        String mciId = "NCDP_CUSAFT10A0";
        MaskingProperties.PathRule rule = new MaskingProperties.PathRule();
        rule.setJsonPath("CUSAFT_IN.CUSAFT_IN_SUB.acno");
        rule.setMaskingType("type_account");

        List<MaskingProperties.PathRule> rules = Arrays.asList(rule);

        // 3. Mocking 설정
        when(maskingManager.getMaskingRules(mciId)).thenReturn(rules);

        // 4. 테스트 대상 메소드 실행
        maskingService.applyMasking(mciId, testData);

        // 5. 결과 검증
        Map<String, Object> cusaftInResult = (Map<String, Object>) testData.get("CUSAFT_IN");
        List<Map<String, Object>> listResult = (List<Map<String, Object>>) cusaftInResult.get("CUSAFT_IN_SUB");

        // List의 모든 요소에 마스킹이 적용되었는지 확인
        assertThat(listResult.get(0).get("acno")).isEqualTo("<Protected_Mci_Data>");
        assertThat(listResult.get(0).get("name")).isEqualTo("김철수"); // 룰이 없는 필드는 그대로

        assertThat(listResult.get(1).get("acno")).isEqualTo("<Protected_Mci_Data>");
        assertThat(listResult.get(1).get("name")).isEqualTo("이영희"); // 룰이 없는 필드는 그대로
    }

    @Test
    @DisplayName("빈 List에 대해 예외가 발생하지 않아야 한다")
    void applyMasking_withEmptyList() {
        // 1. 빈 List를 포함한 테스트 데이터 생성
        Map<String, Object> testData = new HashMap<>();
        Map<String, Object> cusaftIn = new HashMap<>();
        cusaftIn.put("CUSAFT_IN_SUB", Collections.emptyList());
        testData.put("CUSAFT_IN", cusaftIn);

        // 2. 마스킹 룰 준비
        String mciId = "NCDP_CUSAFT10A0";
        MaskingProperties.PathRule rule = new MaskingProperties.PathRule();
        rule.setJsonPath("CUSAFT_IN.CUSAFT_IN_SUB.acno");
        rule.setMaskingType("type_account");

        List<MaskingProperties.PathRule> rules = Arrays.asList(rule);

        // 3. Mocking 설정
        when(maskingManager.getMaskingRules(mciId)).thenReturn(rules);

        // 4. 테스트 대상 메소드 실행 - 예외가 발생하지 않아야 함
        maskingService.applyMasking(mciId, testData);

        // 5. 결과 검증 - List가 여전히 비어있어야 함
        Map<String, Object> cusaftInResult = (Map<String, Object>) testData.get("CUSAFT_IN");
        List<?> listResult = (List<?>) cusaftInResult.get("CUSAFT_IN_SUB");
        assertThat(listResult).isEmpty();
    }

    @Test
    @DisplayName("common 룰과 mciId별 룰이 모두 적용되어야 한다")
    void applyMasking_withCommonAndSpecificRules() {
        // 1. common 필드와 mciId별 필드를 포함한 테스트 데이터 생성
        Map<String, Object> testData = new HashMap<>();

        // common 룰 대상 필드 (pfmidata.rlno, pfminpt.rlno)
        Map<String, Object> pfmidata = new HashMap<>();
        pfmidata.put("rlno", "1234567890123");

        Map<String, Object> pfminpt = new HashMap<>();
        pfminpt.put("rlno", "9876543210987");

        testData.put("pfmidata", pfmidata);
        testData.put("pfminpt", pfminpt);

        // mciId별 룰 대상 필드
        Map<String, Object> mimeinInMst = new HashMap<>();
        Map<String, Object> mimeinIn = new HashMap<>();
        mimeinIn.put("custNm", "김철수");
        mimeinInMst.put("MIMEIN_IN", mimeinIn);
        testData.put("MIMEIN_IN_MST", mimeinInMst);

        // 2. common 룰 + mciId별 룰 준비
        String mciId = "NCDP_MIMEIN10A0";

        // common 룰
        MaskingProperties.PathRule commonRule1 = new MaskingProperties.PathRule();
        commonRule1.setJsonPath("pfmidata.rlno");
        commonRule1.setMaskingType("type_rlno");

        MaskingProperties.PathRule commonRule2 = new MaskingProperties.PathRule();
        commonRule2.setJsonPath("pfminpt.rlno");
        commonRule2.setMaskingType("type_rlno");

        // mciId별 룰
        MaskingProperties.PathRule specificRule = new MaskingProperties.PathRule();
        specificRule.setJsonPath("MIMEIN_IN_MST.MIMEIN_IN.custNm");
        specificRule.setMaskingType("type_name");

        // common + specific 룰을 합친 리스트
        List<MaskingProperties.PathRule> allRules = Arrays.asList(commonRule1, commonRule2, specificRule);

        // 3. Mocking 설정: MaskingManager가 common + specific 룰을 반환하도록 설정
        when(maskingManager.getMaskingRules(mciId)).thenReturn(allRules);

        // 4. 테스트 대상 메소드 실행
        maskingService.applyMasking(mciId, testData);

        // 5. 결과 검증
        // 5-1. common 룰이 적용되었는지 확인
        Map<String, Object> pfmidataResult = (Map<String, Object>) testData.get("pfmidata");
        assertThat(pfmidataResult.get("rlno")).isEqualTo("<Protected_Mci_Data>");

        Map<String, Object> pfminptResult = (Map<String, Object>) testData.get("pfminpt");
        assertThat(pfminptResult.get("rlno")).isEqualTo("<Protected_Mci_Data>");

        // 5-2. mciId별 룰이 적용되었는지 확인
        Map<String, Object> mimeinInResult = (Map<String, Object>) ((Map<String, Object>) testData.get("MIMEIN_IN_MST")).get("MIMEIN_IN");
        assertThat(mimeinInResult.get("custNm")).isEqualTo("<Protected_Mci_Data>");
    }

    @Test
    @DisplayName("common 룰만 존재하고 mciId별 룰이 없어도 정상 동작해야 한다")
    void applyMasking_withOnlyCommonRules() {
        // 1. common 필드만 포함한 테스트 데이터 생성
        Map<String, Object> testData = new HashMap<>();

        Map<String, Object> pfmidata = new HashMap<>();
        pfmidata.put("rlno", "1234567890123");
        pfmidata.put("otherField", "untouched");

        testData.put("pfmidata", pfmidata);

        // 2. common 룰만 준비
        String mciId = "UNKNOWN_MCI_ID"; // 존재하지 않는 mciId

        MaskingProperties.PathRule commonRule = new MaskingProperties.PathRule();
        commonRule.setJsonPath("pfmidata.rlno");
        commonRule.setMaskingType("type_rlno");

        List<MaskingProperties.PathRule> rules = Arrays.asList(commonRule);

        // 3. Mocking 설정: common 룰만 반환
        when(maskingManager.getMaskingRules(mciId)).thenReturn(rules);

        // 4. 테스트 대상 메소드 실행
        maskingService.applyMasking(mciId, testData);

        // 5. 결과 검증
        Map<String, Object> pfmidataResult = (Map<String, Object>) testData.get("pfmidata");
        assertThat(pfmidataResult.get("rlno")).isEqualTo("<Protected_Mci_Data>");
        assertThat(pfmidataResult.get("otherField")).isEqualTo("untouched"); // 룰이 없는 필드는 그대로
    }

    @Test
    @DisplayName("중첩된 List 구조(List 내부의 List)에서도 마스킹이 적용되어야 한다")
    void applyMasking_withNestedListStructure() {
        // 1. 중첩된 List 구조를 포함한 테스트 데이터 생성
        // MIMEIN10A0.svcidata.sub01.subsub01.rlno
        Map<String, Object> testData = new HashMap<>();
        Map<String, Object> mimein10a0 = new HashMap<>();
        Map<String, Object> svcidata = new HashMap<>();

        // sub01 - List 타입
        List<Map<String, Object>> sub01List = new ArrayList<>();

        // sub01의 첫 번째 요소
        Map<String, Object> sub01Item1 = new HashMap<>();

        // subsub01 - List 타입 (중첩된 List)
        List<Map<String, Object>> subsub01List1 = new ArrayList<>();

        Map<String, Object> subsub01Item1 = new HashMap<>();
        subsub01Item1.put("rlno", "1111111111111");
        subsub01Item1.put("name", "홍길동");
        subsub01List1.add(subsub01Item1);

        Map<String, Object> subsub01Item2 = new HashMap<>();
        subsub01Item2.put("rlno", "2222222222222");
        subsub01Item2.put("name", "김철수");
        subsub01List1.add(subsub01Item2);

        sub01Item1.put("subsub01", subsub01List1);
        sub01List.add(sub01Item1);

        // sub01의 두 번째 요소
        Map<String, Object> sub01Item2 = new HashMap<>();

        List<Map<String, Object>> subsub01List2 = new ArrayList<>();

        Map<String, Object> subsub01Item3 = new HashMap<>();
        subsub01Item3.put("rlno", "3333333333333");
        subsub01Item3.put("name", "이영희");
        subsub01List2.add(subsub01Item3);

        sub01Item2.put("subsub01", subsub01List2);
        sub01List.add(sub01Item2);

        svcidata.put("sub01", sub01List);
        mimein10a0.put("svcidata", svcidata);
        testData.put("MIMEIN10A0", mimein10a0);

        // 2. 중첩된 경로에 대한 마스킹 룰 준비
        String mciId = "NCDP_MIMEIN10A0";
        MaskingProperties.PathRule rule = new MaskingProperties.PathRule();
        rule.setJsonPath("MIMEIN10A0.svcidata.sub01.subsub01.rlno");
        rule.setMaskingType("type_rlno");

        List<MaskingProperties.PathRule> rules = Arrays.asList(rule);

        // 3. Mocking 설정
        when(maskingManager.getMaskingRules(mciId)).thenReturn(rules);

        // 4. 테스트 대상 메소드 실행
        maskingService.applyMasking(mciId, testData);

        // 5. 결과 검증 - 중첩된 List 구조 깊이 탐색
        Map<String, Object> mimein10a0Result = (Map<String, Object>) testData.get("MIMEIN10A0");
        Map<String, Object> svcidataResult = (Map<String, Object>) mimein10a0Result.get("svcidata");
        List<Map<String, Object>> sub01Result = (List<Map<String, Object>>) svcidataResult.get("sub01");

        // sub01의 첫 번째 요소 검증
        List<Map<String, Object>> subsub01Result1 = (List<Map<String, Object>>) sub01Result.get(0).get("subsub01");
        assertThat(subsub01Result1.get(0).get("rlno")).isEqualTo("<Protected_Mci_Data>");
        assertThat(subsub01Result1.get(0).get("name")).isEqualTo("홍길동"); // 룰이 없는 필드는 그대로
        assertThat(subsub01Result1.get(1).get("rlno")).isEqualTo("<Protected_Mci_Data>");
        assertThat(subsub01Result1.get(1).get("name")).isEqualTo("김철수"); // 룰이 없는 필드는 그대로

        // sub01의 두 번째 요소 검증
        List<Map<String, Object>> subsub01Result2 = (List<Map<String, Object>>) sub01Result.get(1).get("subsub01");
        assertThat(subsub01Result2.get(0).get("rlno")).isEqualTo("<Protected_Mci_Data>");
        assertThat(subsub01Result2.get(0).get("name")).isEqualTo("이영희"); // 룰이 없는 필드는 그대로
    }

    @Test
    @DisplayName("3단계 이상 중첩된 List 구조에서도 마스킹이 적용되어야 한다")
    void applyMasking_withDeeplyNestedListStructure() {
        // 1. 3단계 중첩 구조: root.level1.level2.level3.rlno
        Map<String, Object> testData = new HashMap<>();
        Map<String, Object> root = new HashMap<>();

        // level1 - List
        List<Map<String, Object>> level1List = new ArrayList<>();
        Map<String, Object> level1Item = new HashMap<>();

        // level2 - List (내부 List)
        List<Map<String, Object>> level2List = new ArrayList<>();
        Map<String, Object> level2Item = new HashMap<>();

        // level3 - List (더 깊은 내부 List)
        List<Map<String, Object>> level3List = new ArrayList<>();

        Map<String, Object> level3Item1 = new HashMap<>();
        level3Item1.put("rlno", "9999999999999");
        level3Item1.put("data", "test1");

        Map<String, Object> level3Item2 = new HashMap<>();
        level3Item2.put("rlno", "8888888888888");
        level3Item2.put("data", "test2");

        level3List.add(level3Item1);
        level3List.add(level3Item2);

        level2Item.put("level3", level3List);
        level2List.add(level2Item);

        level1Item.put("level2", level2List);
        level1List.add(level1Item);

        root.put("level1", level1List);
        testData.put("root", root);

        // 2. 마스킹 룰 준비
        String mciId = "TEST_DEEP_NESTED";
        MaskingProperties.PathRule rule = new MaskingProperties.PathRule();
        rule.setJsonPath("root.level1.level2.level3.rlno");
        rule.setMaskingType("type_rlno");

        List<MaskingProperties.PathRule> rules = Arrays.asList(rule);

        // 3. Mocking 설정
        when(maskingManager.getMaskingRules(mciId)).thenReturn(rules);

        // 4. 테스트 대상 메소드 실행
        maskingService.applyMasking(mciId, testData);

        // 5. 결과 검증 - 3단계 중첩 구조 탐색
        Map<String, Object> rootResult = (Map<String, Object>) testData.get("root");
        List<Map<String, Object>> level1Result = (List<Map<String, Object>>) rootResult.get("level1");
        List<Map<String, Object>> level2Result = (List<Map<String, Object>>) level1Result.get(0).get("level2");
        List<Map<String, Object>> level3Result = (List<Map<String, Object>>) level2Result.get(0).get("level3");

        assertThat(level3Result.get(0).get("rlno")).isEqualTo("<Protected_Mci_Data>");
        assertThat(level3Result.get(0).get("data")).isEqualTo("test1"); // 룰이 없는 필드는 그대로
        assertThat(level3Result.get(1).get("rlno")).isEqualTo("<Protected_Mci_Data>");
        assertThat(level3Result.get(1).get("data")).isEqualTo("test2"); // 룰이 없는 필드는 그대로
    }
}