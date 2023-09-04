package com.lkd.bt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by lkkings on 2023/8/24
 * 异常状态枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorEnum implements CodeEnum<String> {
    SUCCESS("0000","成功"),
    COMMON_ERROR("0001","通用异常"),
    UNKNOWN_ERROR("0002","未知异常"),
    FORM_ERROR("1001", "参数校验异常"),
    ;
    private final String code;
    private final String message;

}
