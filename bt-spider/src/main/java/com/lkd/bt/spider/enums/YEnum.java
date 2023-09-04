package com.lkd.bt.spider.enums;

import com.lkd.bt.common.enums.CodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by lkkings on 2023/8/24
 * krpc回应枚举
 */
@AllArgsConstructor
@Getter
public enum YEnum implements CodeEnum<String> {
    QUERY("q", "请求"),
    RECEIVE("r", "回复"),
    ERROR("e", "异常"),
    ;

    private String code;
    private String message;
}
