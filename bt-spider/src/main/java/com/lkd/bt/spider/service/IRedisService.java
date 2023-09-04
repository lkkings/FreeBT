package com.lkd.bt.spider.service;
import org.redisson.api.*;

import java.util.List;
import java.util.Map;

/**
 * @author lkd
 * @date 2022/6/8
 */
public interface IRedisService {
    /**
     * 暴露redisson的RMapCache对象
     * @param key
     * @return
     */
    RMapCache<String,Object> getRedisMap(String key);

    /**
     * 存入数据到RMapCache对象
     * @param key
     * @param field
     * @param value
     */
    Object put(String key,String field,Object value);

    /**
     * 移除数据
     * @param key
     * @param field
     */
    Object remove(String key,String field);

    /**
     * 获取RMapCache对象存储数据大小
     * @param key
     * @return
     */
    long size(String key);

    /**
     * 获取RMapCache对象所有的值
     * @param key
     * @return
     */
    List<Object> getValues(String key);

    /**
     * 检查RMapCache对象是否包含该属性
     * @param key
     * @param filed
     * @return
     */
    boolean containField(String key,String filed);

    /**
     * 检查RMapCache对象是否包含该值
     * @param key
     * @param value
     * @return
     */
    boolean containValue(String key,Object value);

    /**
     * 获取RMapCache对象属性的值
     * @param key
     * @param filed
     * @return
     */
    Object get(String key,String filed);

    /**
     * 暴露RHyperLogLog<T>对象
     * @param key
     * @return
     */
    <T> RHyperLogLog<T> getHLL(String key);

    /**
     * 存入数据到RHyperLogLog对象
     * @param key
     * @param value
     * @return
     */
    boolean add(String key,Object value);
}