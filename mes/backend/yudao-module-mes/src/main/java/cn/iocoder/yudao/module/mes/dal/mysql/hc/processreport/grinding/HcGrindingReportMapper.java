package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingReportDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcGrindingReportMapper extends BaseMapperX<HcGrindingReportDO> {

    default HcGrindingReportDO selectLatestByPlanOperationId(Long planOperationId) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingReportDO>()
                .eq(HcGrindingReportDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingReportDO::getDeleted, false)
                .orderByDesc(HcGrindingReportDO::getId)
                .last("LIMIT 1"));
    }

    default HcGrindingReportDO selectLatestSubmittedByPlanOperationId(Long planOperationId) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingReportDO>()
                .eq(HcGrindingReportDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingReportDO::getReportType, "END")
                .eq(HcGrindingReportDO::getDeleted, false)
                .orderByDesc(HcGrindingReportDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcGrindingReportDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingReportDO>()
                .eq(HcGrindingReportDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingReportDO::getDeleted, false)
                .orderByDesc(HcGrindingReportDO::getId));
    }
}
