package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordPageReqVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcSlittingSliceRecordMapper extends BaseMapperX<HcSlittingSliceRecordDO> {

    /** 日报以首次实际消耗时间归属，旧数据回退扫码确认时间，不使用创建/修改时间。 */
    default List<HcSlittingSliceRecordDO> selectProductionRecordSlices(HcSlittingPressProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcSlittingSliceRecordDO> wrapper = new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                .eq(HcSlittingSliceRecordDO::getDeleted, false)
                .eq(HcSlittingSliceRecordDO::getScanStatus, "CONFIRMED");
        String businessTime = "COALESCE(source_consume_time, scan_time)";
        if (reqVO.getReportDateStart() != null) {
            wrapper.apply(businessTime + " >= {0}", reqVO.getReportDateStart().atStartOfDay());
        }
        if (reqVO.getReportDateEnd() != null) {
            wrapper.apply(businessTime + " < {0}", reqVO.getReportDateEnd().plusDays(1).atStartOfDay());
        }
        return selectList(wrapper.orderByAsc(HcSlittingSliceRecordDO::getId));
    }

    default List<HcSlittingSliceRecordDO> selectListByPlanOperationId(Long planOperationId) {
        return selectListByPlanOperationId(planOperationId, null, null);
    }

    default List<HcSlittingSliceRecordDO> selectListByPlanOperationId(Long planOperationId,
                                                                      LocalDateTime scanTimeStart,
                                                                      LocalDateTime scanTimeEndExclusive) {
        return selectList(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                .eq(HcSlittingSliceRecordDO::getPlanOperationId, planOperationId)
                .geIfPresent(HcSlittingSliceRecordDO::getScanTime, scanTimeStart)
                .ltIfPresent(HcSlittingSliceRecordDO::getScanTime, scanTimeEndExclusive)
                .eq(HcSlittingSliceRecordDO::getDeleted, false)
                .orderByAsc(HcSlittingSliceRecordDO::getSourceAdhesiveReportId)
                .orderByAsc(HcSlittingSliceRecordDO::getSliceIndex));
    }

    /**
     * 查询分切工序全部历史切片（包含逻辑删除记录），仅用于追溯与审计，不能作为新切片的占号依据。
     */
    @Select("""
            SELECT *
            FROM mes_sfc_slitting_slice_record
            WHERE plan_operation_id = #{planOperationId}
            ORDER BY source_adhesive_report_id ASC, slice_index ASC
            """)
    List<HcSlittingSliceRecordDO> selectHistoryListByPlanOperationId(
            @Param("planOperationId") Long planOperationId);

    default HcSlittingSliceRecordDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                .eq(HcSlittingSliceRecordDO::getId, id)
                .eq(HcSlittingSliceRecordDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default List<HcSlittingSliceRecordDO> selectListBySourceId(Long planOperationId, Long sourceAdhesiveReportId) {
        return selectList(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                .eq(HcSlittingSliceRecordDO::getPlanOperationId, planOperationId)
                .eq(HcSlittingSliceRecordDO::getSourceAdhesiveReportId, sourceAdhesiveReportId)
                .eq(HcSlittingSliceRecordDO::getDeleted, false)
                .orderByAsc(HcSlittingSliceRecordDO::getSliceIndex));
    }

    default List<HcSlittingSliceRecordDO> selectListBySourceAdhesiveReportId(Long sourceAdhesiveReportId) {
        return selectList(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                .eq(HcSlittingSliceRecordDO::getSourceAdhesiveReportId, sourceAdhesiveReportId)
                .eq(HcSlittingSliceRecordDO::getDeleted, false)
                .orderByAsc(HcSlittingSliceRecordDO::getPlanOperationId)
                .orderByAsc(HcSlittingSliceRecordDO::getSliceIndex));
    }

    default HcSlittingSliceRecordDO selectBySerialNo(String sliceSerialNo) {
        return selectOne(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                .eq(HcSlittingSliceRecordDO::getSliceSerialNo, sliceSerialNo)
                .eq(HcSlittingSliceRecordDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default HcSlittingSliceRecordDO selectConfirmedBySerialNo(String sliceSerialNo) {
        return selectOne(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                .eq(HcSlittingSliceRecordDO::getSliceSerialNo, sliceSerialNo)
                .eq(HcSlittingSliceRecordDO::getScanStatus, "CONFIRMED")
                .eq(HcSlittingSliceRecordDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default List<HcSlittingSliceRecordDO> selectConfirmedListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                .eq(HcSlittingSliceRecordDO::getPlanId, planId)
                .eq(HcSlittingSliceRecordDO::getScanStatus, "CONFIRMED")
                .eq(HcSlittingSliceRecordDO::getDeleted, false)
                .orderByAsc(HcSlittingSliceRecordDO::getSourceProductionBatchNo)
                .orderByAsc(HcSlittingSliceRecordDO::getSliceIndex));
    }

    default List<HcSlittingSliceRecordDO> selectAbnormalListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                .eq(HcSlittingSliceRecordDO::getPlanId, planId)
                .eq(HcSlittingSliceRecordDO::getDeleted, false)
                .and(wrapper -> wrapper
                        .in(HcSlittingSliceRecordDO::getSelfCheck, "NG", "ABNORMAL", "FAILED", "不合格", "异常")
                        .or()
                        .like(HcSlittingSliceRecordDO::getVisualResultJson, "NG"))
                .orderByAsc(HcSlittingSliceRecordDO::getSourceProductionBatchNo)
                .orderByAsc(HcSlittingSliceRecordDO::getSliceIndex));
    }
}
