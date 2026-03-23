package com.itgeo.enums;

/**
 * @author gzx
 * @description: SSE消息类型
 * @date 2024-05-20 10:00:00
 */
public enum PriceCompareEnum {

    GREATER_THAN(">", "大于"),
    LESS_THAN("<", "小于"),
    GREATER_THAN_OR_EQUAL_TO(">=", "大于等于"),
    LESS_THAN_OR_EQUAL_TO("<=", "小于等于"),

    HIGHER_THAN(">", "高于"),
    LOWER_THAN("<", "低于"),
    NOT_HIGHER_THAN("<=", "不高于"),
    NOT_LOWER_THAN(">=", "不低于"),

    EQUAL_TO("=", "等于");

    public final String type;
    public final String value;

    PriceCompareEnum(String type, String value) {
        this.type = type;
        this.value = value;
    }

}
