package com.lkd.bt.spider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lkd.bt.spider.entity.InfoHash;
import com.lkd.bt.spider.mapper.InfoHashMapper;
import com.lkd.bt.spider.service.IInfoHashService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (infoHash == null) {
            //如果为空,则新建
            infoHash = InfoHash.builder()
                    .infoHash(infoHashHexStr)
                    .peerAddress(peersStr)
                    .build();
        } else if(StringUtils.isEmpty(infoHash.getPeerAddress()) || infoHash.getPeerAddress().split(";").length <= 16){
            //如果不为空,并且长度小于一定值,则追加
            infoHash.setPeerAddress(infoHash.getPeerAddress()+ peersStr);
        }
        this.save(infoHash);
    }
}
