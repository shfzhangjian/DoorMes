package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgOutboundBoxDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcFgOutboundBoxMapper extends BaseMapperX<HcFgOutboundBoxDO> {

    default List<HcFgOutboundBoxDO> selectListByOutboundOrderId(Long outboundOrderId) {
        return selectList(new LambdaQueryWrapperX<HcFgOutboundBoxDO>()
                .eq(HcFgOutboundBoxDO::getOutboundOrderId, outboundOrderId)
                .eq(HcFgOutboundBoxDO::getDeleted, false)
                .orderByAsc(HcFgOutboundBoxDO::getId));
    }

    default HcFgOutboundBoxDO selectByBoxNo(String outboundBoxNo) {
        return selectOne(new LambdaQueryWrapperX<HcFgOutboundBoxDO>()
                .eq(HcFgOutboundBoxDO::getOutboundBoxNo, outboundBoxNo)
                .eq(HcFgOutboundBoxDO::getDeleted, false)
                .orderByDesc(HcFgOutboundBoxDO::getId)
                .last("LIMIT 1"));
    }
}
