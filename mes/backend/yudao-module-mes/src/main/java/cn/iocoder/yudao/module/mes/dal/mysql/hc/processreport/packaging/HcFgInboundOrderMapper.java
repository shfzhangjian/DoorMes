package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgInboundOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcFgInboundOrderMapper extends BaseMapperX<HcFgInboundOrderDO> {

    default HcFgInboundOrderDO selectByInboundNo(String inboundNo) {
        return selectOne(new LambdaQueryWrapperX<HcFgInboundOrderDO>()
                .eq(HcFgInboundOrderDO::getInboundNo, inboundNo)
                .eq(HcFgInboundOrderDO::getDeleted, false)
                .orderByDesc(HcFgInboundOrderDO::getId)
                .last("LIMIT 1"));
    }
}
