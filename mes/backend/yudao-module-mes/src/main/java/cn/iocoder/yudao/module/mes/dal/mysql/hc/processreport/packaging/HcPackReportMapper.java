package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackReportDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPackReportMapper extends BaseMapperX<HcPackReportDO> {

    default HcPackReportDO selectLatestByPlanOperationId(Long planOperationId) {
        return selectOne(new LambdaQueryWrapperX<HcPackReportDO>()
                .eq(HcPackReportDO::getPlanOperationId, planOperationId)
                .eq(HcPackReportDO::getDeleted, false)
                .orderByDesc(HcPackReportDO::getId)
                .last("LIMIT 1"));
    }
}
