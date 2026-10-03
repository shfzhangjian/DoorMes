package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionDetailDO;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcCutRoundInspectionDetailMapper extends BaseMapperX<HcCutRoundInspectionDetailDO> {

    default List<HcCutRoundInspectionDetailDO> selectListByTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<HcCutRoundInspectionDetailDO>()
                .in(HcCutRoundInspectionDetailDO::getTaskId, taskIds)
                .eq(HcCutRoundInspectionDetailDO::getDeleted, false)
                .orderByAsc(HcCutRoundInspectionDetailDO::getTaskId)
                .orderByAsc(HcCutRoundInspectionDetailDO::getSeqNo)
                .orderByAsc(HcCutRoundInspectionDetailDO::getId));
    }

    default List<HcCutRoundInspectionDetailDO> selectListByTaskId(Long taskId) {
        if (taskId == null) {
            return Collections.emptyList();
        }
        return selectListByTaskIds(List.of(taskId));
    }

    default List<HcCutRoundInspectionDetailDO> selectListByCutRoundReportId(Long cutRoundReportId) {
        if (cutRoundReportId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<HcCutRoundInspectionDetailDO>()
                .eq(HcCutRoundInspectionDetailDO::getCutRoundReportId, cutRoundReportId)
                .eq(HcCutRoundInspectionDetailDO::getDeleted, false)
                .orderByAsc(HcCutRoundInspectionDetailDO::getSeqNo)
                .orderByAsc(HcCutRoundInspectionDetailDO::getId));
    }

    default List<HcCutRoundInspectionDetailDO> selectListByFqcOrderId(Long fqcOrderId) {
        if (fqcOrderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<HcCutRoundInspectionDetailDO>()
                .eq(HcCutRoundInspectionDetailDO::getFqcOrderId, fqcOrderId)
                .eq(HcCutRoundInspectionDetailDO::getDeleted, false)
                .orderByAsc(HcCutRoundInspectionDetailDO::getTaskId)
                .orderByAsc(HcCutRoundInspectionDetailDO::getSeqNo)
                .orderByAsc(HcCutRoundInspectionDetailDO::getId));
    }
}
