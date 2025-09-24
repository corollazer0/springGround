package com.example.playground.service;

import com.example.playground.dto.LombokImmutableDto;
import com.example.playground.dto.LombokMutableDto;
import org.springframework.stereotype.Service;

@Service
public class DtoService {

    public void immutableDto(LombokImmutableDto lombokImmutableDto) {
        System.out.println("immutableDto");

    }

    public void mutableDto(LombokMutableDto lombokMutableDto) {
        System.out.println("mutableDto");

    }
}
