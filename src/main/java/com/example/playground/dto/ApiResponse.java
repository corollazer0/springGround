package com.example.playground.dto;

import lombok.Getter;
import lombok.Value;

@Value
public class ApiResponse<T> {
    boolean success;
    T data;
    String message;

    // 생성자, 정적 팩토리 메서드 등
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}