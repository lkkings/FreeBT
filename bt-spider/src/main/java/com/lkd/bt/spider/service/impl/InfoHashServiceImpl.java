package com.lkd.bt.spider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lkd.bt.spider.entity.InfoHash;
import com.lkd.bt.spider.mapper.InfoHashMapper;
import com.lkd.bt.spider.service.IInfoHashService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * <p>
 * info_hash 服务实现类
 * </p>
 *
 * @author lkkings
 * @since 2023-09-03
 */
@Service
public class InfoHashServiceImpl extends ServiceImpl<InfoHashMapper, InfoHash> implements IInfoHashService {
    @Transactional
    @Override
    public void saveInfoHash(String infoHashHexStr,String peersStr) {
        InfoHash infoHash = this.getOne(new LambdaQueryWrapper<InfoHash>().eq(InfoHash::getInfoHash,infoHashHexStr));
        StringBuilder address = new StringBuilder();
        if (Objects.nonNull(infoHash)){
            address.append(infoHash.getPeerAddress());
        }
        infoHash = InfoHash.builder()
                .infoHash(infoHashHexStr)
                .peerAddress(address.append(peersStr).toString())
                .build();
        this.saveOrUpdate(infoHash);
    }
}
