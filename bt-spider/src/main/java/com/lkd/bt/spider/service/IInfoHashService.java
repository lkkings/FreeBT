package com.lkd.bt.spider.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lkd.bt.spider.entity.InfoHash;

/**
 * <p>
 * info_hash 服务类
 * </p>
 *
 * @author lkkings
 * @since 2023-09-03
 */
public interface IInfoHashService extends IService<InfoHash> {
    void saveInfoHash(String infoHashHexStr,String peersStr);
}
