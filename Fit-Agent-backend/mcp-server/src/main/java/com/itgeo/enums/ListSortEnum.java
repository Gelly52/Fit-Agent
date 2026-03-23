package com.itgeo.enums;

/**
 * @author gzx
 * @description: SSE消息类型
 * @date 2024-05-20 10:00:00
 */
public enum ListSortEnum {

    ASC("asc", "升序排序"),
    DESC("desc", "降序排序");

    public final String type;
    public final String value;

    ListSortEnum(String type, String value) {
        this.type = type;
        this.value = value;
    }

}
