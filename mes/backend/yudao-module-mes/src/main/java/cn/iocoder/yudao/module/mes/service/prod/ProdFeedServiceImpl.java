// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.prod.ProdFeedServiceImpl.java
package cn.iocoder.yudao.module.mes.service.prod;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.prod.vo.ProdFeedVOs.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.prod.ProdFeedDO;
import cn.iocoder.yudao.module.mes.dal.mysql.prod.ProdFeedMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class ProdFeedServiceImpl {
    @Resource
    private ProdFeedMapper prodFeedMapper;

    public Long createProdFeed(SaveReqVO createReqVO) {
        ProdFeedDO feedDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, ProdFeedDO.class);
        if (feedDO.getFeedTime() == null) feedDO.setFeedTime(LocalDateTime.now());
        prodFeedMapper.insert(feedDO);
        return feedDO.getId();
    }

    public PageResult<ProdFeedDO> getProdFeedPage(PageReqVO pageReqVO) {
        return prodFeedMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<ProdFeedDO>()
                .eqIfPresent(ProdFeedDO::getScheduleId, pageReqVO.getScheduleId())
                .eqIfPresent(ProdFeedDO::getMaterialId, pageReqVO.getMaterialId())
                .eqIfPresent(ProdFeedDO::getLotNo, pageReqVO.getLotNo())
                .orderByDesc(ProdFeedDO::getId));
    }
}
