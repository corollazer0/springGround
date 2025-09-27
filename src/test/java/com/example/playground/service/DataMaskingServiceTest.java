package com.example.playground.service;

import com.example.playground.service.Impl.DataMaskingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class DataMaskingServiceTest {

    private DataMaskingService dataMaskingService;

    @BeforeEach
    void setUp() {
        dataMaskingService = new DataMaskingServiceImpl();
    }

    @Test
    @DisplayName("기본 마스킹 테스트 - 배열 형태 대괄호")
    void testBasicMaskingWithSquareBrackets() {
        String input = "[{abc=value1},{test=value2}]";
        String expected = "[{abc=<Masked>}, {test=value2}]";
        String actual = dataMaskingService.maskSensitiveData(input);
        assertThat(actual).isEqualToIgnoringWhitespace(expected);
    }

    @Test
    @DisplayName("기본 마스킹 테스트 - JSON 형태 중괄호")
    void testBasicMaskingWithCurlyBraces() {
        String input = "{abc=value1, test=value2}";
        String expected = "{abc=<Masked>, test=value2}";
        String actual = dataMaskingService.maskSensitiveData(input);
        assertThat(actual).isEqualToIgnoringWhitespace(expected);
    }

    @Test
    @DisplayName("다중 민감키 마스킹 테스트")
    void testMultipleSensitiveKeys() {
        String input = "[{abc=secret}, {xyz=password}, {test=value}]";
        String expected = "[{abc=<Masked>}, {xyz=<Masked>}, {test=value}]";
        String actual = dataMaskingService.maskSensitiveData(input);
        assertThat(actual).isEqualToIgnoringWhitespace(expected);
    }

    @Test
    @DisplayName("재귀적 마스킹 테스트 - 중첩 구조(2depth)")
    void testRecursiveMasking() {
        String input = "[{abc=value1}, {nested={xyz=secret, test=value2}}]";
        String expected = "[{abc=<Masked>}, {nested={xyz=<Masked>, test=value2}}]";
        String actual = dataMaskingService.maskSensitiveData(input);
        assertThat(actual).isEqualToIgnoringWhitespace(expected);
    }

    @Test
    @DisplayName("깊은 중첩 구조 테스트(3depth)")
    void testDeepNestedStructure() {
        String input = "{outer=val, abc=sec, inner=[{xyz=pass}, {deep={abc=secret}}]}";
        String expected = "{outer=val, abc=<Masked>, inner=[{xyz=<Masked>}, {deep={abc=<Masked>}}]}";
        String actual = dataMaskingService.maskSensitiveData(input);
        assertThat(actual).isEqualToIgnoringWhitespace(expected);
    }

    @Test
    @DisplayName("카드 코어 연동 표준 중첩 구조 테스트(MIMEIN15A0_IN_MST)")
    void testCardCoreStructure() {
        String input = "{MIMEIN15A0_IN_MST=" +
                            "{MIMEIN15A0_IN=" +
                                "{cd_cusno=322110113, " +
                                 "MIMEIN15A0_subArray01=[{cd_no=12341234, mbepay_dsc=1}, {cd_no=56785678, mbepay_dsc=1}, {cd_no=56785678, mbepay_dsc=1}]}}" +
                            ", pfmidata={std_glbl_id=238489238402, svc_id=MIMEIN15A0, sync_dsc=R}}";
        String expected = "{MIMEIN15A0_IN_MST=" +
                "{MIMEIN15A0_IN=" +
                "{cd_cusno=322110113, " +
                 "MIMEIN15A0_subArray01=[{cd_no=<Masked>, mbepay_dsc=1}, {cd_no=<Masked>, mbepay_dsc=1}, {cd_no=<Masked>, mbepay_dsc=1}]}}" +
                ", pfmidata={std_glbl_id=238489238402, svc_id=MIMEIN15A0, sync_dsc=<Masked>}}";
        String actual = dataMaskingService.maskSensitiveData(input);
        assertThat(actual).isEqualToIgnoringWhitespace(expected);
    }

    @Test
    @DisplayName("빈 문자열 처리")
    void testEmptyString() {
        assertThat(dataMaskingService.maskSensitiveData(null)).isNull();
        assertThat(dataMaskingService.maskSensitiveData("")).isEqualTo("");
    }
}
