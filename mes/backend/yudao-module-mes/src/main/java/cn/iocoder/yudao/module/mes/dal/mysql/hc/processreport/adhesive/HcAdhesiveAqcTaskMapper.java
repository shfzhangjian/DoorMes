package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveAqcTaskDO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcAdhesiveAqcTaskMapper extends BaseMapperX<HcAdhesiveAqcTaskDO> {

    default List<HcAdhesiveAqcTaskDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveAqcTaskDO>()
                .eq(HcAdhesiveAqcTaskDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesiveAqcTaskDO::getDeleted, false)
                .orderByDesc(HcAdhesiveAqcTaskDO::getId));
    }

    default HcAdhesiveAqcTaskDO selectLatestDailyTask(Long planOperationId, String glueBoardBatchNo, LocalDate recordDate) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveAqcTaskDO>()
                .eq(HcAdhesiveAqcTaskDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesiveAqcTaskDO::getGlueBoardBatchNo, glueBoardBatchNo)
                .eq(HcAdhesiveAqcTaskDO::getTaskType, "DAILY_GLUE_BOARD")
                .eq(HcAdhesiveAqcTaskDO::getRecordDate, recordDate)
                .eq(HcAdhesiveAqcTaskDO::getDeleted, false)
                .orderByDesc(HcAdhesiveAqcTaskDO::getId)
                .last("LIMIT 1"));
    }

    default HcAdhesiveAqcTaskDO selectLatestByPlanOperationUsageAndDate(Long planOperationId,
                                                                        Long glueBoardUsageId,
                                                                        String taskType,
                                                                        LocalDate recordDate) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveAqcTaskDO>()
                .eq(HcAdhesiveAqcTaskDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesiveAqcTaskDO::getGlueBoardUsageId, glueBoardUsageId)
                .eq(HcAdhesiveAqcTaskDO::getTaskType, taskType)
                .eq(HcAdhesiveAqcTaskDO::getRecordDate, recordDate)
                .eq(HcAdhesiveAqcTaskDO::getDeleted, false)
                .orderByDesc(HcAdhesiveAqcTaskDO::getId)
                .last("LIMIT 1"));
    }

    default HcAdhesiveAqcTaskDO selectLatestByUsageAndDate(Long glueBoardUsageId,
                                                           String taskType,
                                                           LocalDate recordDate) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveAqcTaskDO>()
                .eq(HcAdhesiveAqcTaskDO::getGlueBoardUsageId, glueBoardUsageId)
                .eq(HcAdhesiveAqcTaskDO::getTaskType, taskType)
                .eqIfPresent(HcAdhesiveAqcTaskDO::getRecordDate, recordDate)
                .eq(HcAdhesiveAqcTaskDO::getDeleted, false)
                .orderByDesc(HcAdhesiveAqcTaskDO::getId)
                .last("LIMIT 1"));
    }

    default HcAdhesiveAqcTaskDO selectLatestReportTask(Long adhesiveReportId) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveAqcTaskDO>()
                .eq(HcAdhesiveAqcTaskDO::getAdhesiveReportId, adhesiveReportId)
                .eq(HcAdhesiveAqcTaskDO::getTaskType, "REPORT_FIRST_INSPECTION")
                .eq(HcAdhesiveAqcTaskDO::getDeleted, false)
                .orderByDesc(HcAdhesiveAqcTaskDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcAdhesiveAqcTaskDO> selectReportTasksByBatch(Long planOperationId,
                                                               String sourceProductionBatchNo,
                                                               LocalDate recordDate) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveAqcTaskDO>()
                .eq(HcAdhesiveAqcTaskDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesiveAqcTaskDO::getGlueBoardBatchNo, sourceProductionBatchNo)
                .eq(HcAdhesiveAqcTaskDO::getTaskType, "REPORT_FIRST_INSPECTION")
                .eqIfPresent(HcAdhesiveAqcTaskDO::getRecordDate, recordDate)
                .eq(HcAdhesiveAqcTaskDO::getDeleted, false)
                .orderByDesc(HcAdhesiveAqcTaskDO::getId));
    }
}
