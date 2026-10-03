// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.wms.WmsOutboundServiceImpl.java
package cn.iocoder.yudao.module.mes.service.wms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsOutboundVOs.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsOutboundDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wms.WmsOutboundMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.annotation.Resource;

@Service
@Validated
public class WmsOutboundServiceImpl {
    @Resource
    private WmsOutboundMapper outboundMapper;

    public Long createOutbound(SaveReqVO createReqVO) {
        WmsOutboundDO outboundDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, WmsOutboundDO.class);
        if (outboundDO.getStatus() == null) outboundDO.setStatus("CREATED");
        outboundMapper.insert(outboundDO);
        return outboundDO.getId();
    }

    public PageResult<WmsOutboundDO> getOutboundPage(PageReqVO pageReqVO) {
        return outboundMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<WmsOutboundDO>()
                .likeIfPresent(WmsOutboundDO::getOutboundNo, pageReqVO.getOutboundNo())
                .eqIfPresent(WmsOutboundDO::getType, pageReqVO.getType())
                .eqIfPresent(WmsOutboundDO::getWorkOrderNo, pageReqVO.getWorkOrderNo())
                .eqIfPresent(WmsOutboundDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(WmsOutboundDO::getId));
    }
}
