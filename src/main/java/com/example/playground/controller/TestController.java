package com.example.playground.controller;

import com.example.playground.dto.ApiResponse;
import com.example.playground.dto.LombokImmutableDto;
import com.example.playground.dto.LombokMutableDto;
import com.example.playground.service.DtoService;
import com.example.playground.service.TestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
@Slf4j
@RestController
public class TestController {

    private final TestService testService;
    private final DtoService dtoService;
    private final ObjectMapper mapper;
    public TestController(TestService testService, DtoService dtoService, ObjectMapper mapper) {
        this.testService = testService;
        this.dtoService = dtoService;
        this.mapper = mapper;
    }


    @PostMapping("/api")
    public String api(@RequestBody Map<String, Object> requestBody) {
        testService.sayHello();
        log.info("Test API {}", requestBody);
        return "OK";
    }

    @PostMapping("/api/lombok/immutable")
    public ResponseEntity<ApiResponse<LombokImmutableDto>> apiImmutableDto(@RequestBody LombokImmutableDto lombokImmutableDto) {
        dtoService.immutableDto(lombokImmutableDto);
        log.info("lombokImmutableDto API {}", lombokImmutableDto.toString());

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(lombokImmutableDto));
    }

    @PostMapping("/api/lombok/mutable")
    public ResponseEntity<ApiResponse<LombokMutableDto>> apiMutableDto(@RequestBody LombokMutableDto lombokMutableDto) {
        dtoService.mutableDto(lombokMutableDto);
        log.info("lombokMutableDto API {}", lombokMutableDto.toString());

        System.out.println(lombokMutableDto.toString());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(lombokMutableDto));
    }
    //HashMap to JSON Serialization for Server to Server Data Send
    //map과 objectMapper의 writeValueAsString를 사용한다.
    @PostMapping("/api/jsonobject/maptojsonstring")
    public ResponseEntity<ApiResponse<String>> maptojsonstring(@RequestBody LombokMutableDto lombokMutableDto) throws JsonProcessingException {

        String jsonString = mapper.writeValueAsString(lombokMutableDto);
        log.info("apimaptojson API {}", jsonString);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(jsonString));
    }

    //JSON to Map DeSerialization
    @PostMapping("/api/jsonobject/jsonstringtomap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> jsonstringtomap(@RequestBody String jsonString) throws JsonProcessingException {

        Map<String, Object> dataMap = mapper.readValue(jsonString, new TypeReference<>() {});
        log.info("jsontomap API {}", dataMap.toString());

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(dataMap));
    }
}

/*
writeValueAsString(Object value): Java 객체 → JSON 문자열
MyDto dto = new MyDto("test", 123);
String jsonString = objectMapper.writeValueAsString(dto);
// 결과: "{\"name\":\"test\",\"value\":123}"

readValue(String content, Class<T> valueType): JSON 문자열 → Java 객체
String jsonString = "{\"name\":\"test\",\"value\":123}";
MyDto dto = objectMapper.readValue(jsonString, MyDto.class);

convertValue(Object fromValue, Class<T> toValueType): Java 객체 ↔ 다른 Java 객체
Map<String, Object> dataMap = new HashMap<>();
dataMap.put("name", "test");
dataMap.put("value", 123);
// Map을 MyDto 객체로 직접 변환
MyDto dto = objectMapper.convertValue(dataMap, MyDto.class);

readTree(String content)
String jsonString = "{\"user\":{\"name\":\"test\",\"details\":{\"age\":30}}}";
JsonNode rootNode = objectMapper.readTree(jsonString);
String name = rootNode.get("user").get("name").asText(); // "test"
int age = rootNode.path("user").path("details").path("age").asInt(); // 30
*/

/*
Kafka 설정
spring:
  kafka:
    producer:
      # 메시지 키는 보통 String을 사용합니다.
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      # 메시지 값(Value)은 JsonSerializer를 사용해 자동으로 JSON 변환!
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer


ASIS
@Service
public class ManualKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public void sendLog(LogDto logDto) throws JsonProcessingException {
        // 1. DTO를 JSON 문자열로 수동 변환
        String jsonString = mapper.writeValueAsString(logDto);

        // 2. 변환된 문자열을 전송
        kafkaTemplate.send("logs", jsonString);
    }
}

TOBE
@Service
public class AutoKafkaProducer {

    // KafkaTemplate의 Value 타입을 DTO 객체로 지정합니다.
    private final KafkaTemplate<String, LogDto> kafkaTemplate;

    // ... 생성자 생략 ...

    public void sendLog(LogDto logDto) {
        // DTO 객체를 그냥 그대로 전송!
        // Spring이 알아서 JSON으로 변환해줍니다.
        kafkaTemplate.send("logs", logDto);
    }
}
 */