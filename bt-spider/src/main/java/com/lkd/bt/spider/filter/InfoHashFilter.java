package com.lkd.bt.spider.filter;

import com.clearspring.analytics.stream.cardinality.HyperLogLog;
import com.google.common.hash.BloomFilter;
import com.lkd.bt.spider.config.Config;
import com.lkd.bt.spider.service.impl.MetadataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by lkkings on 2023/8/27
 */

@Component
@Slf4j
public class InfoHashFilter {
    private static final String LOG = "[InfoHashFilter]";

    private volatile HyperLogLog filter;



    private volatile boolean available = false;

    private final MetadataServiceImpl metadataService;

    private final Config config;

    public InfoHashFilter(Config config, MetadataServiceImpl metadataService) {
        this.config = config;
        this.metadataService = metadataService;
    }

    public boolean put(String infoHash){
        return filter.offer(infoHash);
    }
    public void enable(){
        if (!this.available)
            importExistInfoHash();
    }

    /**
     * 导入所有入库种子信息到过滤器
     */
    private void importExistInfoHash() {
        try {
            log.info("{}正在初始化过滤器...",LOG);
            filter = new HyperLogLog(config.getPerformance().getInfoHashFilterFpp());
            int start = 0;
            //每次查询条数
            int all_size = 0,queried_size = 0;
            int size = config.getEs().getImportExistInfoHashPageSize();
            List<String> infoHashList;
            do {
                infoHashList =metadataService.getBaseMapper().getInfoHashByPage(start,start+size);
                infoHashList.forEach(infoHash->filter.offer(infoHash));
                start += size;
                queried_size = infoHashList.size();
                all_size += queried_size;
            }
            while (queried_size == size);
            available = true;
            log.info("{}初始化完成.当前总长度:{}",LOG,all_size);
        } catch (Exception e) {
            log.error("{}初始化失败,程序崩溃.异常:{}", e.getMessage(), e);
        }
    }
}
