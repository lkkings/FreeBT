package service.impl;

import entity.InfoHash;
import mapper.InfoHashMapper;
import service.IInfoHashService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

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

}
