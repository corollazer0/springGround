package com.example.playground.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // 핵심 어노테이션
@NoArgsConstructor // JSON 변환이나 JPA 사용 시 필요
@AllArgsConstructor // 모든 필드를 포함하는 생성자
public class LombokMutableDto {

    private String cusno;
    private String name;

}
