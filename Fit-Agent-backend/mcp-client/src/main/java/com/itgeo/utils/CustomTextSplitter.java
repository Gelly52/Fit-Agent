package com.itgeo.utils;

import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.List;

/*
* 自定义文本切分器
* */
public class CustomTextSplitter extends TextSplitter {
    @Override
    protected List<String> splitText(String text) {
        return List.of(split(text));
    }

    public String[] split(String text) {
        return text.split("\\s*\\R\\s*\\R\\s*"); //正则表达式表示的是匹配一个或多个换行符（包括空格）
    }
}
