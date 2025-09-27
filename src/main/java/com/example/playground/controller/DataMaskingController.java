package com.example.playground.controller;

import com.example.playground.service.DataMaskingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data")
public class DataMaskingController {

    private final DataMaskingService dataMaskingService;

    @Autowired
    public DataMaskingController(DataMaskingService dataMaskingService) {
        this.dataMaskingService = dataMaskingService;
    }

    @PostMapping("/mask")
    public ResponseEntity<String> maskData(@RequestBody String rawData) {
        String maskedData = dataMaskingService.maskSensitiveData(rawData);
        return ResponseEntity.ok(maskedData);
    }
}