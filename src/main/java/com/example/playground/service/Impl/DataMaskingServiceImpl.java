package com.example.playground.service.Impl;


import com.example.playground.service.DataMaskingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 데이터 마스킹 서비스 구현체
 */
@Service
public class DataMaskingServiceImpl implements DataMaskingService {

    private static final String MASKED_VALUE = "<Masked>";
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList("abc", "xyz"));
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("(\\w+)=([^,\\[\\]{}]+|\\[[^\\]]*\\]|\\{[^}]*\\})");

    @Override
    public String maskSensitiveData(String data) {
        if (!StringUtils.hasText(data)) {
            return data;
        }

        // 시작/종료 괄호 추출
        char startBracket = data.charAt(0);
        char endBracket = data.charAt(data.length() - 1);

        if (!isValidBracket(startBracket, endBracket)) {
            return data;
        }

        // 괄호 내부 데이터 추출
        String content = data.substring(1, data.length() - 1).trim();

        // 재귀적 마스킹 처리
        String maskedContent = processContent(content);

        // 원본 괄호 형태로 재조립
        return startBracket + maskedContent + endBracket;
    }

    /**
     * 컨텐츠 내부의 key=value 쌍을 처리
     */
    private String processContent(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }

        StringBuilder result = new StringBuilder();
        Matcher matcher = KEY_VALUE_PATTERN.matcher(content);
        int lastEnd = 0;

        while (matcher.find()) {
            // 매칭 사이의 구분자(쉼표, 공백 등) 추가
            result.append(content, lastEnd, matcher.start());

            String key = matcher.group(1);
            String value = matcher.group(2);

            result.append(key).append("=");

            // 민감한 키인 경우 마스킹
            if (SENSITIVE_KEYS.contains(key)) {
                result.append(MASKED_VALUE);
            }
            // value가 중첩된 구조인 경우 재귀 처리
            else if (isNestedStructure(value)) {
                result.append(maskSensitiveData(value));
            }
            // 일반 값
            else {
                result.append(value);
            }

            lastEnd = matcher.end();
        }

        // 남은 부분 추가
        result.append(content.substring(lastEnd));

        return result.toString();
    }

    /**
     * 유효한 괄호 쌍인지 확인
     */
    private boolean isValidBracket(char start, char end) {
        return (start == '[' && end == ']') || (start == '{' && end == '}');
    }

    /**
     * 중첩된 구조([...] 또는 {...})인지 확인
     */
    private boolean isNestedStructure(String value) {
        if (value.length() < 2) {
            return false;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        return isValidBracket(first, last);
    }
}