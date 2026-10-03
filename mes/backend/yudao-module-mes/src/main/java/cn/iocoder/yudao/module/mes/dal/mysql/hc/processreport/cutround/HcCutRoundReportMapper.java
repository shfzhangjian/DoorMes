package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcCutRoundReportMapper extends BaseMapperX<HcCutRoundReportDO> {

    default List<HcCutRoundReportDO> selectListByPlanOperationId(Long planOperationId) {
        return selectListByPlanOperationId(planOperationId, null, null);
    }

    default List<HcCutRoundReportDO> selectListByPlanOperationId(Long planOperationId,
                                                                LocalDateTime scanConfirmStart,
                                                                LocalDateTime scanConfirmEndExclusive) {
        return selectList(new LambdaQueryWrapperX<HcCutRoundReportDO>()
                .eq(HcCutRoundReportDO::getPlanOperationId, planOperationId)
                .geIfPresent(HcCutRoundReportDO::getConfirmerTime, scanConfirmStart)
                .ltIfPresent(HcCutRoundReportDO::getConfirmerTime, scanConfirmEndExclusive)
                .eq(HcCutRoundReportDO::getDeleted, false)
                .orderByDesc(HcCutRoundReportDO::getId));
    }

    default List<HcCutRoundReportDO> selectListBySourceAdhesive2ReportId(Long sourceAdhesive2ReportId) {
        return selectList(new LambdaQueryWrapperX<HcCutRoundReportDO>()
                .eq(HcCutRoundReportDO::getSourceAdhesive2ReportId, sourceAdhesive2ReportId)
                .eq(HcCutRoundReportDO::getDeleted, false)
                .orderByAsc(HcCutRoundReportDO::getId));
    }

    default List<HcCutRoundReportDO> selectListBySourceAdhesive2ReportIds(List<Long> sourceAdhesive2ReportIds) {
        if (sourceAdhesive2ReportIds == null || sourceAdhesive2ReportIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcCutRoundReportDO>()
                .in(HcCutRoundReportDO::getSourceAdhesive2ReportId, sourceAdhesive2ReportIds)
                .eq(HcCutRoundReportDO::getDeleted, false)
                .orderByDesc(HcCutRoundReportDO::getId));
    }

    default HcCutRoundReportDO selectByProductionBatchNo(String productionBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcCutRoundReportDO>()
                .eq(HcCutRoundReportDO::getProductionBatchNo, productionBatchNo)
                .eq(HcCutRoundReportDO::getDeleted, false)
                .orderByDesc(HcCutRoundReportDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcCutRoundReportDO> selectListByBatchNo(String batchNo) {
        return selectList(new LambdaQueryWrapperX<HcCutRoundReportDO>()
                .eq(HcCutRoundReportDO::getDeleted, false)
                .and(wrapper -> wrapper.eq(HcCutRoundReportDO::getProductionBatchNo, batchNo)
                        .or()
                        .eq(HcCutRoundReportDO::getSourceProductionBatchNo, batchNo))
                .orderByDesc(HcCutRoundReportDO::getId));
    }

    default List<HcCutRoundReportDO> selectListByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<HcCutRoundReportDO>()
                .in(HcCutRoundReportDO::getId, ids)
                .eq(HcCutRoundReportDO::getDeleted, false));
    }

    default HcCutRoundReportDO selectByIdForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcCutRoundReportDO>()
                .eq(HcCutRoundReportDO::getId, id)
                .eq(HcCutRoundReportDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default List<HcCutRoundReportDO> selectProductionRecordList(HcCutRoundProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcCutRoundReportDO> wrapper = new LambdaQueryWrapperX<HcCutRoundReportDO>()
                .eq(HcCutRoundReportDO::getDeleted, false)
                .in(HcCutRoundReportDO::getReportStatus, List.of("SUBMITTED", "CONFIRMED"))
                .likeIfPresent(HcCutRoundReportDO::getModelCode, reqVO.getModelCode())
                .likeIfPresent(HcCutRoundReportDO::getRecorderName, reqVO.getRecorderName());
        if (reqVO.getReportDateStart() != null) {
            wrapper.apply("COALESCE(report_date, DATE(confirmer_time), DATE(end_time), DATE(recorder_time)) >= {0}",
                    reqVO.getReportDateStart());
        }
        if (reqVO.getReportDateEnd() != null) {
            wrapper.apply("COALESCE(report_date, DATE(confirmer_time), DATE(end_time), DATE(recorder_time)) <= {0}",
                    reqVO.getReportDateEnd());
        }
        if (reqVO.getProductionBatchNo() != null && !reqVO.getProductionBatchNo().isBlank()) {
            wrapper.and(item -> item
                    .like(HcCutRoundReportDO::getProductionBatchNo, reqVO.getProductionBatchNo())
                    .or()
                    .like(HcCutRoundReportDO::getParentProductionBatchNo, reqVO.getProductionBatchNo())
                    .or()
                    .like(HcCutRoundReportDO::getSourceProductionBatchNo, reqVO.getProductionBatchNo())
                    .or()
                    .like(HcCutRoundReportDO::getSourceBatchNo, reqVO.getProductionBatchNo()));
        }
        return selectList(wrapper
                .orderByAsc(HcCutRoundReportDO::getReportDate)
                .orderByAsc(HcCutRoundReportDO::getModelCode)
                .orderByAsc(HcCutRoundReportDO::getParentProductionBatchNo)
                .orderByAsc(HcCutRoundReportDO::getSourceBatchNo)
                .orderByAsc(HcCutRoundReportDO::getProductionBatchNo)
                .orderByAsc(HcCutRoundReportDO::getId));
    }

}
