package com.example.playground.config;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class MaskingManager {
    // 1. MaskingProperties에서 이미 완성된 Map을 가지고 있음
    private final MaskingProperties maskingProperties;

    // 생성자 주입
    public MaskingManager(MaskingProperties maskingProperties) {
        this.maskingProperties = maskingProperties;
    }

    /**
     * 외부에서 사용할 메소드
     * @param mciId 연동 ID
     * @return 해당 MCIId에 적용할 마스킹 룰 목록 (없으면 빈 리스트)
     */
    public List<MaskingProperties.PathRule> getMaskingRules(String mciId) {
        // 3. Properties 객체가 가진 Map을 직접 조회
        return maskingProperties.getMappings().getOrDefault(mciId, Collections.emptyList());
    }
}
