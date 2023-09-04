package com.lkd.bt.spider.util;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.lkd.bt.common.exception.BTException;
import com.lkd.bt.common.util.MArrayUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Bencode 编解码类
 */
@Component
public class Bencode {
    private static final String LOG = "[Bencode]";

    //函数数组
    @SuppressWarnings("unchecked")
    private final BiFunction<byte[], Integer, MethodResult<?>>[] functions = new BiFunction[4];
    //":"字符串转byte
    private byte stringTypeSeparator;

    //bencode编码中的若干类型前缀和后缀------------
    private final String intTypePre = "i";
    private final String listTypePre = "l";
    private final String dictTypePre = "d";
    private final String typeSuf = "e";

    @Autowired
    public void init() {
        stringTypeSeparator = ":".getBytes()[0];
        //使用方法引用简写
        functions[0] = this::decodeDict;
        functions[1] = this::decodeString;
        functions[2] = this::decodeInt;
        functions[3] = this::decodeList;
    }

    /**
     * String 解码
     * 从byte[]的指定位置开始
     */
    public MethodResult<String> decodeString(byte[] bytes, int start) {
        if (start >= bytes.length || bytes[start] < '0' || bytes[start] > '9')
            throw new BTException(LOG + "解码String类型异常,start异常. start:" + start);
        //从指定位置开始向后查找冒号,如果没有返回-1
        int separatorIndex = MArrayUtil.indexOf(bytes, stringTypeSeparator,start);
        if (separatorIndex == -1)
            throw new BTException(LOG + "解码String类型异常,无法找到分隔符");
        //截取开始位置 到 冒号的字节, 转为数字,也就是该字符的长度
        int strLen;
        try {
            strLen = Integer.parseInt(new String(ArrayUtil.sub( bytes,start, separatorIndex)));
        } catch (NumberFormatException e) {
            throw new BTException(LOG + "解码String类型异常,长度非int类型");
        }
        if (strLen < 0)
            throw new BTException(LOG + "解码String类型异常,长度小于0");
        //字符结束位置索引
        int endIndex = separatorIndex + strLen + 1;
        if (separatorIndex > bytes.length)
            throw new BTException(LOG + "解码String类型异常,长度超出");
        return new MethodResult<>(new String(ArrayUtil.sub(bytes, separatorIndex + 1, endIndex)), endIndex);
    }

    /**
     * int 解码
     * 从指定位置开始
     */
    public MethodResult<Long> decodeInt(byte[] bytes, int start) {
        if (start >= bytes.length || bytes[start] != intTypePre.charAt(0))
            throw new BTException(LOG + "解码Int类型异常,start异常. start:" + start);
        //结束索引
        int endIndex = MArrayUtil.indexOf(bytes, typeSuf.getBytes()[0],start);
        if (endIndex == -1)
            throw new BTException(LOG + "解码Int类型异常,无法找到结束符");
        long result;
        try {
            //此处的解码必须为long,因为metadata中的一些数值可能超过int.
            //该类的其他int是没事的,因为只是索引大小.暂时不做字节长度超过int的考虑
            result = Long.parseLong(new String(ArrayUtil.sub(bytes, start + 1, endIndex)));
        } catch (NumberFormatException e) {
            throw new BTException(LOG + "解码Int类型异常,值非int类型");
        }
        return new MethodResult<>(result, ++endIndex);
    }

    /**
     * list 解码
     */
    public MethodResult<List<Object>> decodeList(byte[] bytes, int start) {
        List<Object> result = new ArrayList<>();
        if (start >= bytes.length || bytes[start] != listTypePre.charAt(0))
            throw new BTException(LOG + "解码List类型异常,start异常. start:" + start);
        //循环l后面的每个字节
        int i = start + 1;
        while (i < bytes.length) {
            //如果是结束字符,退出循环
            if (bytes[i] == typeSuf.getBytes()[0])
                break;
            //解码为任意类型
            MethodResult<?> methodResult = decodeAny(bytes, i);
            //索引由具体解码方法控制
            i = methodResult.index;
            //增加到结果
            result.add(methodResult.value);
        }
        if (i == bytes.length)
            throw new BTException(LOG + "解码List类型异常,结束符不存在");
        return new MethodResult<>(result, ++i);
    }

    /**
     * dict解码
     */
    public MethodResult<Map<String, Object>> decodeDict(byte[] bytes, int start) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (start >= bytes.length || bytes[start] != dictTypePre.charAt(0))
            throw new BTException(LOG + "解码Dict类型异常,start异常. start:" + start);

        //循环d后面的每个字节
        int i = start + 1;
        while (i < bytes.length) {
            String item = new String(new byte[]{bytes[i]});
            //如果是结束字符,退出循环
            if (item.equals(typeSuf))
                break;
            //如果不为数字,格式异常(因为每次循环解析一个key/value对,而key是string类型,string类型的编码是长度在前)
            if (!StrUtil.isNumeric(item))
                throw new BTException(LOG + "解码Dict异常,key/value对非数字开头");
            //解析key
            MethodResult<String> keyMethodResult = decodeString(bytes, i);
            //更新索引
            i = keyMethodResult.index;
            //解析value
            MethodResult<?> valueMethodResult = decodeAny(bytes, i);
            //更新索引
            i = valueMethodResult.index;
            //放入
            result.put(keyMethodResult.value, valueMethodResult.value);
        }
        if (i == bytes.length)
            throw new BTException(LOG + "解码Dict类型异常,结束符不存在");
        return new MethodResult<>(result, ++i);
    }

    /**
     * 任意类型解码
     */
    public MethodResult<?> decodeAny(byte[] bytes, int start) {
        for (BiFunction<byte[], Integer, MethodResult<?>> function : functions) {
            try {
                return function.apply(bytes, start);
            } catch (Exception ignored) {
            }
        }
            throw new BTException(LOG + "解码失败.start:" + start +",bytes:" + new String(bytes));

    }

    /**
     * 封装任意类型解码
     * @param tClass 返回的类型 可选Map/String/Integer/List
     */
    @SuppressWarnings("unchecked")
    public <T> T decode(byte[] bytes, Class<T> tClass) {
        return (T) decodeAny(bytes, 0).value;
    }


    //编码相关------------------------------------------------------------------------------------

    /**
     * String 编码
     */
    public String encodeString(String string) {
        return string.length() + ":" + string;
    }

    /**
     * int 编码
     */
    public String encodeLong(long i) {
        return intTypePre + i + typeSuf;
    }

    /**
     * list 编码
     */
    public String encodeList(List<Object> list) {
        String[] result = new String[list.size() + 2];
        result[0] = listTypePre;
        for (int i = 1; i <= list.size(); i++) {
            result[i] = encodeAny(list.get(i - 1));
        }
        result[result.length - 1] = typeSuf;

        return String.join("", result);
    }

    /**
     * dict编码
     */
    public String encodeDict(Map<String, Object> map) {
        String[] result = new String[map.size() + 2];
        result[0] = dictTypePre;
        result[result.length - 1] = typeSuf;
        int i = 1;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result[i++] = encodeString(entry.getKey()) + encodeAny(entry.getValue());
        }
        return String.join("", result);
    }

    /**
     * 任意编码
     */
    @SuppressWarnings("unchecked")
    public String encodeAny(Object obj) {
        try {
            if (obj instanceof Integer) {
                return encodeLong(Integer.toUnsignedLong((int)obj) );
            }else if(obj instanceof Long){
                return encodeLong((long)obj );
            } else if (obj instanceof String) {
                return encodeString((String) obj);
            } else if (obj instanceof Map) {
                return encodeDict((Map<String, Object>) obj);
            } else if (obj instanceof List) {
                return encodeList((List<Object>) obj);
            } else {
                throw new BTException(LOG + "类型无效.当前类型:" + obj.getClass().getName());
            }
        } catch (BTException e) {
            throw e;
        } catch (Exception e) {
            throw new BTException(LOG + "编码异常:" + e.getMessage());
        }
    }

    /**
     * string to bytes
     */
    public byte[] toBytes(String string) {
        return string.getBytes();
    }

    /**
     * 通用编码方式,包装了encodeAny 和 toBytes
     */
    public byte[] encode(Object obj) {
        return toBytes(encodeAny(obj));
    }


    /**
     * 解码方法返回对象, 包括解码结果和当前索引位置
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MethodResult<T> {
        private T value;
        private int index;
    }



}
