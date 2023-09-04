package com.lkd.bt.spider.service.impl;

import com.lkd.bt.spider.service.IRedisService;
import lombok.RequiredArgsConstructor;
import org.redisson.RedissonMap;
import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.redisson.api.*;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.ScoredEntry;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lkkings on 2023/8/27
 */

@Component
@RequiredArgsConstructor
public class RedisServiceImpl implements IRedisService {

    private final RedissonClient redisson;

    @Override
    public RMapCache<String, Object> getRedisMap(String key) {
        return redisson.getMapCache(key);
    }

    @Override
    public Object put(String key, String field, Object value) {
        return getRedisMap(key).put(field,value);
    }

    @Override
    public Object remove(String key, String field) {
        return getRedisMap(key).remove(field);
    }

    @Override
    public long size(String key) {
        return getRedisMap(key).size();
    }

    @Override
    public List<Object> getValues(String key) {
        return Arrays.asList(getRedisMap(key).values().toArray());
    }

    @Override
    public boolean containField(String key, String filed) {
        return getRedisMap(key).containsKey(filed);
    }

    @Override
    public boolean containValue(String key, Object value) {
        return getRedisMap(key).containsValue(value);
    }

    @Override
    public Object get(String key, String filed) {
        return getRedisMap(key).get(filed);
    }

    @Override
    public <T> RHyperLogLog<T> getHLL(String key) {
        return redisson.getHyperLogLog(key);
    }

    @Override
    public boolean add(String key, Object value) {
        return getHLL(key).add(value);
    }
}
