package com.example.playground.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MaskingService {
    private final MaskingManager maskingManager;

    private static final Pattern FIRST_DIGIT_PATTERN = Pattern.compile("\\d");

    public MaskingService(MaskingManager maskingManager) {
        this.maskingManager = maskingManager;
    }



    /**
     * MCIId와 원본 Map 데이터를 받아 마스킹을 적용합니다.
     * (원본 Map 객체가 직접 수정됩니다.)
     */
    public void applyMasking(String mciId, Map<String, Object> dataMap) {
        // 1. Manager에게 "룰"을 요청
        List<MaskingProperties.PathRule> rules = maskingManager.getMaskingRules(mciId);

        if (rules.isEmpty() || dataMap == null || dataMap.isEmpty()) {
            return; // 룰이 없거나 데이터가 없으면 종료
        }

        // 2. 룰을 순회하며 마스킹 "실행"
        for (MaskingProperties.PathRule rule : rules) {
            // 3. jsonPath를 기반으로 Map을 탐색하여 마스킹 적용
            try {
                // "MIMEIN_IN_MST.MIMEIN_IN.rlno" 같은 경로를 "." 기준으로 분리
                String[] keys = rule.getJsonPath().split("\\.");
                maskRecursive(dataMap, keys, 0, rule.getMaskingType());
            } catch (Exception e) {
                // 경로 탐색 중 오류 발생 시 로깅 (예: ClassCastException 등)
                //TODO : Log.error 추가 필요
                //log.error("Failed to mask path: {} for MCI: {}", rule.getJsonPath(), mciId, e);
            }
        }
    }

    /**
     * 재귀적으로 Map을 탐색하며 실제 마스킹을 수행하는 헬퍼 메소드
     */
    @SuppressWarnings("unchecked")
    private void maskRecursive(Map<String, Object> currentMap, String[] keys, int index, String maskingType) {

        if (currentMap == null || index >= keys.length) {
            return;
        }

        String currentKey = keys[index];
        Object value = currentMap.get(currentKey);

        if (value == null) {
            return; // 현재 키에 해당하는 값이 없음
        }

        // 1. 마지막 키(마스킹 대상)에 도달한 경우
        if (index == keys.length - 1) {
            String maskedValue = executeMask(value, maskingType);
            // 2. 원본 Map의 값을 마스킹된 값으로 교체
            currentMap.put(currentKey, maskedValue);
        }
        // 3. 아직 더 깊이 탐색해야 하는 경우
        else if (value instanceof Map) {
            // 다음 레벨의 Map으로 재귀 호출
            maskRecursive((Map<String, Object>) value, keys, index + 1, maskingType);
        }
        // 4. List 타입인 경우 처리
        else if (value instanceof List) {
            List<?> list = (List<?>) value;
            // List 내부의 각 요소에 대해 재귀적으로 마스킹 적용
            for (Object item : list) {
                if (item instanceof Map) {
                    maskRecursive((Map<String, Object>) item, keys, index + 1, maskingType);
                }
            }
        }
        // (Map도 List도 아닌데 경로가 더 있다면 무시)
    }


    /**
     * 실제 마스킹 로직을 수행하는 헬퍼 메소드 (이전과 동일)
     */
    private String executeMask(Object original, String type) {
        if (original == null ) {
            return null;
        }

        String originalString = String.valueOf(original);

        if (originalString.isEmpty()) {
            return originalString;
        }
        /* TODO : 추후 Rule 정의되면 반영
        switch (type) {
            case "type_rlno":
                // 예: 9007021234567 -> 900702******* (뒤 7자리)
                if (originalString.length() > 7) {
                    return originalString.substring(0, originalString.length() - 7) + "*******";
                }
                break;
            case "type_name":
                // 예: 홍길동 -> 홍*동
                if (originalString.length() > 2) {
                    return originalString.substring(0, 1) + "*" + originalString.substring(originalString.length() - 1);
                } else if (originalString.length() == 2) {
                    return originalString.substring(0, 1) + "*";
                }
                break;
            // [추가] 주소 마스킹 로직
            case "type_address":
                Matcher matcher = FIRST_DIGIT_PATTERN.matcher(originalString);

                // 1. 주소에서 첫 번째 숫자를 찾습니다.
                if (matcher.find()) {
                    // 2. 숫자가 시작되는 위치(index)를 가져옵니다.
                    int index = matcher.start();

                    if (index > 0) {
                        // 3. 숫자 앞부분(도로명 등)은 유지하고, 숫자부터 끝까지 마스킹
                        return originalString.substring(0, index) + "****";
                    } else {
                        // 주소가 숫자로 시작하면 (예: "123 서울시...") 전체 마스킹
                        return "********";
                    }
                }
                // 주소에 숫자가 아예 없으면 (예: "서울특별시 강남구") 원본 반환
                return originalString;
        }
        */
        return "<Protected_Mci_Data>"; // 기본 마스킹
    }
}
