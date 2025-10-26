package com.example.playground.controller;

import com.example.playground.config.MaskingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController()
@RequestMapping("/api")
public class MciMaksingController {

    private final MaskingService maskingService;

    @Autowired
    public MciMaksingController(MaskingService maskingService) {
        this.maskingService = maskingService;
    }

   @PostMapping("/maskingTest")
    public Map<String, Object> testMasking(@RequestBody Map<String, Object> input) {

        System.out.println("--- 마스킹 전 원본 데이터 ---");
        System.out.println(input);
        // 출력: {MIMEIN_IN_MST={MIMEIN_IN={custNm=홍길동, rlno=9007021234567}}}


        // 2. MaskingService를 호출하여 마스킹 적용
        // (data 객체가 이 메소드 호출로 인해 직접 변경됩니다.)
        maskingService.applyMasking("NCDP_MIMEIN10A0", input);


        System.out.println("--- 마스킹 후 데이터 ---");
        System.out.println(input);
        return input;
    }
}
