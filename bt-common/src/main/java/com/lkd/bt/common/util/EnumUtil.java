package com.lkd.bt.common.util;

import com.lkd.bt.common.enums.CodeEnum;

import java.util.Optional;

/**
 * 枚举工具类
 */
public class EnumUtil {

    /**
     * 根据Code返回枚举
     */
    public static <T extends CodeEnum<X>,X> Optional<T> getByCode(X code, Class<T> enumClass) {
        for (T each : enumClass.getEnumConstants()) {
            if (each.getCode().equals(code)) {
                return Optional.of(each);
            }
        }
        return Optional.empty();
    }

    /**
     * 根据Code返回枚举
     */
    public static <T extends CodeEnum<String>> Optional<T> getByCodeString(String code, Class<T> enumClass) {
        for (T each : enumClass.getEnumConstants()) {
            if (each.getCode().equalsIgnoreCase(code)) {
                return Optional.of(each);
            }
        }
        return Optional.empty();
    }


    /**
     * 某个 code 和某个枚举的的code是否一致
     * 注意,一定要前者equals后者.否则容易空指针
     */
    public static <T extends CodeEnum<X>,X> boolean equals(X code,T enumObj) {
        return enumObj.getCode().equals(code);
    }


}