package com.example.playground.dto;

public class TestRequestDto {

    private String cusno;
    private String name;

    @Override
    public String toString() {
        return "TestRequestDto{" +
                "cusno='" + cusno + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public void setCusno(String cusno) {
        this.cusno = cusno;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCusno() {
        return cusno;
    }

    public String getName() {
        return name;
    }

    public TestRequestDto(String cusno, String name) {
        this.cusno = cusno;
        this.name = name;
    }
}
