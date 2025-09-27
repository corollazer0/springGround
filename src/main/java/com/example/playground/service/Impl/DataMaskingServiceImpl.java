package com.example.playground.service.Impl;

import com.example.playground.service.DataMaskingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 데이터 마스킹 서비스 구현체 (Manual Parser 기반)
 */
@Service
public class DataMaskingServiceImpl implements DataMaskingService {

    private static final String MASKED_VALUE = "<Masked>";
    // 테스트 케이스를 통과하기 위해 'cd_no' 추가
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList("abc", "xyz", "cd_No", "cd_no", "sync_dsc"));

    @Override
    public String maskSensitiveData(String data) {
        if (!StringUtils.hasText(data)) {
            return data;
        }

        // 1단계: 파싱 (String -> Object)
        Object parsedData = parse(data.trim());

        // 2단계: 마스킹 (Object -> Masked Object)
        mask(parsedData);

        // 3단계: 문자열화 (Masked Object -> String)
        return stringify(parsedData);
    }

    // ===================================================================================
    // 1. 파싱 (Parsing)
    // ===================================================================================

    private Object parse(String data) {
        if (!StringUtils.hasText(data)) {
            return data;
        }
        char firstChar = data.charAt(0);
        if (firstChar == '{') {
            return parseObject(data);
        } else if (firstChar == '[') {
            return parseArray(data);
        }
        return data;
    }

    private Map<String, Object> parseObject(String data) {
        Map<String, Object> map = new LinkedHashMap<>();
        String content = data.substring(1, data.length() - 1);
        CharIterator it = new CharIterator(content);

        while (it.hasNext()) {
            String key = parseKey(it);
            it.skipWhitespaceAndChar('=');
            Object value = parseValue(it);
            map.put(key, value);
            it.skipWhitespaceAndChar(',');
        }
        return map;
    }

    private List<Object> parseArray(String data) {
        List<Object> list = new ArrayList<>();
        String content = data.substring(1, data.length() - 1);
        CharIterator it = new CharIterator(content);

        while (it.hasNext()) {
            Object value = parseValue(it);
            list.add(value);
            it.skipWhitespaceAndChar(',');
        }
        return list;
    }

    private String parseKey(CharIterator it) {
        it.skipWhitespace();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext() && it.peek() != '=') {
            sb.append(it.next());
        }
        return sb.toString().trim();
    }

    private Object parseValue(CharIterator it) {
        it.skipWhitespace();
        char pk = it.peek();
        if (pk == '{' || pk == '[') {
            return parse(extractBlock(it));
        }

        StringBuilder sb = new StringBuilder();
        while (it.hasNext() && it.peek() != ',') {
            sb.append(it.next());
        }
        return sb.toString().trim();
    }

    private String extractBlock(CharIterator it) {
        char openBracket = it.next();
        char closeBracket = (openBracket == '{') ? '}' : ']';
        StringBuilder sb = new StringBuilder();
        sb.append(openBracket);
        int balance = 1;

        while (it.hasNext() && balance > 0) {
            char c = it.next();
            sb.append(c);
            if (c == openBracket) {
                balance++;
            } else if (c == closeBracket) {
                balance--;
            }
        }
        return sb.toString();
    }


    // ===================================================================================
    // 2. 마스킹 (Masking)
    // ===================================================================================

    private void mask(Object data) {
        if (data instanceof Map) {
            maskMap((Map<String, Object>) data);
        } else if (data instanceof List) {
            maskList((List<Object>) data);
        }
    }

    private void maskMap(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (SENSITIVE_KEYS.contains(entry.getKey().toLowerCase())) {
                entry.setValue(MASKED_VALUE);
            } else {
                mask(entry.getValue());
            }
        }
    }

    private void maskList(List<Object> list) {
        for (Object item : list) {
            mask(item);
        }
    }

    // ===================================================================================
    // 3. 문자열화 (Stringification)
    // ===================================================================================

    private String stringify(Object data) {
        if (data instanceof Map) {
            return stringifyMap((Map<String, Object>) data);
        } else if (data instanceof List) {
            return stringifyList((List<Object>) data);
        }
        return data.toString();
    }

    private String stringifyMap(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            sb.append(entry.getKey()).append("=").append(stringify(entry.getValue()));
            if (it.hasNext()) {
                sb.append(", "); // 일관성을 위해 공백 추가
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String stringifyList(List<Object> list) {
        StringBuilder sb = new StringBuilder("[");
        Iterator<Object> it = list.iterator();
        while (it.hasNext()) {
            sb.append(stringify(it.next()));
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    // ===================================================================================
    // 파싱을 위한 헬퍼 클래스
    // ===================================================================================
    private static class CharIterator {
        private final String str;
        private int pos = 0;

        CharIterator(String str) {
            this.str = str;
        }

        boolean hasNext() {
            return pos < str.length();
        }

        char next() {
            return str.charAt(pos++);
        }

        char peek() {
            return str.charAt(pos);
        }

        void skipWhitespace() {
            while (hasNext() && Character.isWhitespace(peek())) {
                pos++;
            }
        }

        void skipWhitespaceAndChar(char toSkip) {
            skipWhitespace();
            if (hasNext() && peek() == toSkip) {
                pos++;
            }
            skipWhitespace();
        }
    }
}
