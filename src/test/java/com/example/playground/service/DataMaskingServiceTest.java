package com.example.playground.service;

import com.example.playground.service.Impl.DataMaskingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class DataMaskingServiceTest {

    private DataMaskingService dataMaskingService;

    @BeforeEach
    void setUp() {
        dataMaskingService = new DataMaskingServiceImpl();
    }

    @Test
    @DisplayName("기본 마스킹 테스트 - 대괄호")
    void testBasicMaskingWithSquareBrackets() {
        String input = "[abc=value1,test=value2]";
        String expected = "[abc=<Masked>,test=value2]";
        assertEquals(expected, dataMaskingService.maskSensitiveData(input));
    }

    @Test
    @DisplayName("기본 마스킹 테스트 - 중괄호")
    void testBasicMaskingWithCurlyBraces() {
        String input = "{abc=value1, test=value2}";
        String expected = "{abc=<Masked>, test=value2}";
        assertEquals(expected, dataMaskingService.maskSensitiveData(input));
    }

    @Test
    @DisplayName("다중 민감키 마스킹 테스트")
    void testMultipleSensitiveKeys() {
        String input = "[abc=secret, xyz=password, test=value]";
        String expected = "[abc=<Masked>, xyz=<Masked>, test=value]";
        assertEquals(expected, dataMaskingService.maskSensitiveData(input));
    }

    @Test
    @DisplayName("재귀적 마스킹 테스트 - 중첩 구조")
    void testRecursiveMasking() {
        String input = "[abc=value1, nested={xyz=secret, test=value2}]";
        String expected = "[abc=<Masked>, nested={xyz=<Masked>, test=value2}]";
        assertEquals(expected, dataMaskingService.maskSensitiveData(input));
    }

    @Test
    @DisplayName("깊은 중첩 구조 테스트")
    void testDeepNestedStructure() {
        String input = "{outer=val, abc=sec, inner=[xyz=pass, deep={abc=secret}]}";
        String expected = "{outer=val, abc=<Masked>, inner=[xyz=<Masked>, deep={abc=<Masked>}]}";
        assertEquals(expected, dataMaskingService.maskSensitiveData(input));
    }

    @Test
    @DisplayName("빈 문자열 처리")
    void testEmptyString() {
        assertNull(dataMaskingService.maskSensitiveData(null));
        assertEquals("", dataMaskingService.maskSensitiveData(""));
    }
}