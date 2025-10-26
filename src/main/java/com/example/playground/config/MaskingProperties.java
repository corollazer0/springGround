package com.example.playground.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map; // 1. Map으로 변경
import java.util.HashMap; // 2. 초기화를 위해 추가

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "masking")
public class MaskingProperties {
    // 3. yml의 "mappings"를 Map으로 직접 바인딩
    // Key: String (MCIId), Value: List<PathRule> (마스킹 룰 리스트)
    private Map<String, List<PathRule>> mappings = new HashMap<>();

    @Getter
    @Setter
    public static class PathRule { // 4. 이름 변경 (mciId가 없으므로)
        // mciId 필드 제거 (Map의 Key로 사용됨)
        private String jsonPath;
        private String maskingType;
    }
}
