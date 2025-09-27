package com.example.playground.service;

/**
 * 데이터 마스킹 서비스 인터페이스
 */
public interface DataMaskingService {
    /**
     * 민감한 키의 값을 마스킹 처리
     *
     * @param data 원본 데이터 문자열
     * @return 마스킹 처리된 문자열
     */
    String maskSensitiveData(String data);
}