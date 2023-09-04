package com.lkd.bt.common.enums;

/**
 * Created by lkkings on 2023/8/24
 * 带code和message的枚举
 */
public interface CodeEnum<T> {
    T getCode();
    String getMessage();
}
