package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgOutboundOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcFgOutboundOrderMapper extends BaseMapperX<HcFgOutboundOrderDO> {

    default HcFgOutboundOrderDO selectByOutboundNo(String outboundNo) {
        return selectOne(new LambdaQueryWrapperX<HcFgOutboundOrderDO>()
                .eq(HcFgOutboundOrderDO::getOutboundNo, outboundNo)
                .eq(HcFgOutboundOrderDO::getDeleted, false)
                .orderByDesc(HcFgOutboundOrderDO::getId)
                .last("LIMIT 1"));
    }

    default HcFgOutboundOrderDO selectBySourceNoticeId(Long sourceNoticeId) {
        return selectOne(new LambdaQueryWrapperX<HcFgOutboundOrderDO>()
                .eq(HcFgOutboundOrderDO::getSourceNoticeId, sourceNoticeId)
                .eq(HcFgOutboundOrderDO::getDeleted, false)
                .orderByDesc(HcFgOutboundOrderDO::getId)
                .last("LIMIT 1"));
    }
}
