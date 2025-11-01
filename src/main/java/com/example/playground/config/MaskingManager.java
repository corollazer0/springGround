package com.example.playground.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MaskingManager {
    // 1. MaskingProperties에서 이미 완성된 Map을 가지고 있음
    private final MaskingProperties maskingProperties;

    // Common 룰을 식별하기 위한 상수
    private static final String COMMON_KEY = "common";

    // 생성자 주입
    public MaskingManager(MaskingProperties maskingProperties) {
        this.maskingProperties = maskingProperties;
    }

    /**
     * 외부에서 사용할 메소드
     * @param mciId 연동 ID
     * @return 해당 MCIId에 적용할 마스킹 룰 목록 (common + mciId별 룰)
     */
    public List<MaskingProperties.PathRule> getMaskingRules(String mciId) {
        List<MaskingProperties.PathRule> result = new ArrayList<>();

        // 1. common 룰 추가 (모든 mciId에 공통 적용)
        List<MaskingProperties.PathRule> commonRules =
            maskingProperties.getMappings().getOrDefault(COMMON_KEY, Collections.emptyList());
        result.addAll(commonRules);

        // 2. mciId별 특정 룰 추가
        List<MaskingProperties.PathRule> specificRules =
            maskingProperties.getMappings().getOrDefault(mciId, Collections.emptyList());
        result.addAll(specificRules);

        return result;
    }
}
