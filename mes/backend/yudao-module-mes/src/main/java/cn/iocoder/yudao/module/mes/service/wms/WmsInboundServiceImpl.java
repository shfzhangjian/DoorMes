// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.wms.WmsInboundServiceImpl.java
package cn.iocoder.yudao.module.mes.service.wms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsInboundSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsInboundPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsInboundDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wms.WmsInboundMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.math.BigDecimal;



@Service
@Validated
class WmsInboundServiceImpl implements WmsInboundService {

    @Resource
    private WmsInboundMapper wmsInboundMapper;

    @Override
    public Long createInbound(WmsInboundSaveReqVO createReqVO) {
        WmsInboundDO inboundDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, WmsInboundDO.class);
        if (inboundDO.getStatus() == null) {
            inboundDO.setStatus("CREATED");
        }
        if (inboundDO.getActualQty() == null) {
            inboundDO.setActualQty(BigDecimal.ZERO);
        }
        wmsInboundMapper.insert(inboundDO);
        return inboundDO.getId();
    }

    @Override
    public void updateInbound(WmsInboundSaveReqVO updateReqVO) {
        WmsInboundDO existDO = wmsInboundMapper.selectById(updateReqVO.getId());
        if (existDO == null) {
            throw new RuntimeException("该入库单不存在！");
        }
        if ("DONE".equals(existDO.getStatus())) {
            throw new RuntimeException("已完成的入库单禁止修改！");
        }
        WmsInboundDO updateObj = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(updateReqVO, WmsInboundDO.class);
        wmsInboundMapper.updateById(updateObj);
    }

    @Override
    public PageResult<WmsInboundDO> getInboundPage(WmsInboundPageReqVO pageReqVO) {
        return wmsInboundMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<WmsInboundDO>()
                .likeIfPresent(WmsInboundDO::getInboundNo, pageReqVO.getInboundNo())
                .eqIfPresent(WmsInboundDO::getType, pageReqVO.getType())
                .likeIfPresent(WmsInboundDO::getSourceNo, pageReqVO.getSourceNo())
                .eqIfPresent(WmsInboundDO::getMaterialId, pageReqVO.getMaterialId())
                .eqIfPresent(WmsInboundDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(WmsInboundDO::getId));
    }
}
