package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingProductionRecordDO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcGrindingProductionLedgerMapper extends BaseMapperX<HcGrindingProductionRecordDO> {

    default PageResult<HcGrindingProductionRecordDO> selectPage(HcGrindingProductionRecordPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcGrindingProductionRecordDO> selectList(HcGrindingProductionRecordPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcGrindingProductionRecordDO> buildQuery(HcGrindingProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcGrindingProductionRecordDO> wrapper = new LambdaQueryWrapperX<HcGrindingProductionRecordDO>()
                .geIfPresent(HcGrindingProductionRecordDO::getRecordTime, reqVO.getCompletionTimeStart())
                .leIfPresent(HcGrindingProductionRecordDO::getRecordTime, reqVO.getCompletionTimeEnd())
                .likeIfPresent(HcGrindingProductionRecordDO::getModelCode, reqVO.getModelCode())
                .likeIfPresent(HcGrindingProductionRecordDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(HcGrindingProductionRecordDO::getMotherBatchNo, reqVO.getMotherBatchNo())
                .eqIfPresent(HcGrindingProductionRecordDO::getRecordRole, reqVO.getRecordRole())
                .eqIfPresent(HcGrindingProductionRecordDO::getSourceBizType, reqVO.getSourceBizType())
                .eqIfPresent(HcGrindingProductionRecordDO::getSegmentMark, reqVO.getSegmentMark())
                .eqIfPresent(HcGrindingProductionRecordDO::getPassType, reqVO.getGrindingPass())
                .likeIfPresent(HcGrindingProductionRecordDO::getRecorderName, reqVO.getRecorderName())
                .eqIfPresent(HcGrindingProductionRecordDO::getStatus, reqVO.getStatus());
        if ("UNCLASSIFIED".equals(reqVO.getPadType())) {
            wrapper.isNull(HcGrindingProductionRecordDO::getPadType);
        } else {
            wrapper.eqIfPresent(HcGrindingProductionRecordDO::getPadType, reqVO.getPadType());
        }
        if (reqVO.getBatchNo() != null && !reqVO.getBatchNo().isBlank()) {
            wrapper.and(item -> item
                    .like(HcGrindingProductionRecordDO::getBatchNo, reqVO.getBatchNo())
                    .or()
                    .like(HcGrindingProductionRecordDO::getMotherBatchNo, reqVO.getBatchNo()));
        }
        return wrapper.orderByDesc(HcGrindingProductionRecordDO::getRecordTime)
                .orderByDesc(HcGrindingProductionRecordDO::getId);
    }

    default HcGrindingProductionRecordDO selectByBizKey(Long equipmentId, LocalDate reportDate, String modelCode,
            String materialCode, String batchNo, String passType, String recordRole, String segmentMark,
            String sandpaperBatchNo, String guideClothBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingProductionRecordDO>()
                .eqIfPresent(HcGrindingProductionRecordDO::getEquipmentId, equipmentId)
                .isNull(equipmentId == null, HcGrindingProductionRecordDO::getEquipmentId)
                .eq(HcGrindingProductionRecordDO::getReportDate, reportDate)
                .eq(HcGrindingProductionRecordDO::getModelCode, modelCode)
                .eq(HcGrindingProductionRecordDO::getMaterialCode, materialCode)
                .eq(HcGrindingProductionRecordDO::getBatchNo, batchNo)
                .eq(HcGrindingProductionRecordDO::getPassType, passType)
                .eq(recordRole != null, HcGrindingProductionRecordDO::getRecordRole, recordRole)
                .isNull(recordRole == null, HcGrindingProductionRecordDO::getRecordRole)
                .eq(segmentMark != null, HcGrindingProductionRecordDO::getSegmentMark, segmentMark)
                .isNull(segmentMark == null, HcGrindingProductionRecordDO::getSegmentMark)
                .eq(HcGrindingProductionRecordDO::getSandpaperBatchNo, sandpaperBatchNo)
                .eq(HcGrindingProductionRecordDO::getGuideClothBatchNo, guideClothBatchNo)
                .last("LIMIT 1"));
    }

    default HcGrindingProductionRecordDO selectBySource(String passType, Long detailId) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingProductionRecordDO>()
                .eq(HcGrindingProductionRecordDO::getPassType, passType)
                .eq(HcGrindingProductionRecordDO::getSourceDetailId, detailId)
                .last("LIMIT 1"));
    }

    default List<HcGrindingProductionRecordDO> selectListBySource(String passType, Long detailId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingProductionRecordDO>()
                .eq(HcGrindingProductionRecordDO::getPassType, passType)
                .eq(HcGrindingProductionRecordDO::getSourceDetailId, detailId)
                .orderByAsc(HcGrindingProductionRecordDO::getSourceSegmentNo)
                .orderByAsc(HcGrindingProductionRecordDO::getId));
    }

    default List<HcGrindingProductionRecordDO> selectListBySource(String sourceBizType, String passType,
                                                                   Long detailId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingProductionRecordDO>()
                .eq(HcGrindingProductionRecordDO::getSourceBizType, sourceBizType)
                .eq(HcGrindingProductionRecordDO::getPassType, passType)
                .eq(HcGrindingProductionRecordDO::getSourceDetailId, detailId)
                .orderByAsc(HcGrindingProductionRecordDO::getSourceSegmentNo)
                .orderByAsc(HcGrindingProductionRecordDO::getId));
    }

    /** 查询同一次手工研发砂纸更换生成的旧/新两条记录。 */
    default List<HcGrindingProductionRecordDO> selectListByManualSplitGroupNo(String manualSplitGroupNo) {
        return selectList(new LambdaQueryWrapperX<HcGrindingProductionRecordDO>()
                .eq(HcGrindingProductionRecordDO::getManualSplitGroupNo, manualSplitGroupNo)
                .eq(HcGrindingProductionRecordDO::getDeleted, false)
                .orderByAsc(HcGrindingProductionRecordDO::getSourceSegmentNo)
                .orderByAsc(HcGrindingProductionRecordDO::getId));
    }

    /**
     * 查询同设备在目标完工时间（含）之前的最新耗材快照。
     * 同一报工如发生砂纸更换，sourceSegmentNo=2 表示新砂纸，优先级高于旧砂纸记录。
     */
    default HcGrindingProductionRecordDO selectLatestConsumableSnapshot(Long equipmentId,
                                                                          LocalDateTime completionTime) {
        return selectLatestConsumableSnapshot(equipmentId, completionTime, null);
    }

    default HcGrindingProductionRecordDO selectLatestConsumableSnapshot(Long equipmentId,
                                                                          LocalDateTime completionTime,
                                                                          Long excludeId) {
        if (equipmentId == null || completionTime == null) {
            return null;
        }
        LambdaQueryWrapperX<HcGrindingProductionRecordDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(HcGrindingProductionRecordDO::getEquipmentId, equipmentId);
        wrapper.ge(HcGrindingProductionRecordDO::getRecordTime, LocalDateTime.of(2000, 1, 1, 0, 0));
        wrapper.le(HcGrindingProductionRecordDO::getRecordTime, completionTime);
        if (excludeId != null) {
            wrapper.ne(HcGrindingProductionRecordDO::getId, excludeId);
        }
        return selectOne(wrapper.orderByDesc(HcGrindingProductionRecordDO::getRecordTime)
                .orderByDesc(HcGrindingProductionRecordDO::getSourceSegmentNo)
                .orderByDesc(HcGrindingProductionRecordDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 查询设备最近一条已确认生产记录，用于研发手工记录确认后回写设备耗材累计值。
     */
    default HcGrindingProductionRecordDO selectLatestConfirmedByEquipment(Long equipmentId) {
        if (equipmentId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcGrindingProductionRecordDO>()
                .eq(HcGrindingProductionRecordDO::getEquipmentId, equipmentId)
                .eq(HcGrindingProductionRecordDO::getStatus, "CONFIRMED")
                .isNotNull(HcGrindingProductionRecordDO::getRecordTime)
                .orderByDesc(HcGrindingProductionRecordDO::getRecordTime)
                .orderByDesc(HcGrindingProductionRecordDO::getSourceSegmentNo)
                .orderByDesc(HcGrindingProductionRecordDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcGrindingProductionRecordDO> selectByRecordIds(Collection<Long> ids) {
        return ids == null || ids.isEmpty() ? List.of()
                : selectList(new LambdaQueryWrapperX<HcGrindingProductionRecordDO>()
                        .in(HcGrindingProductionRecordDO::getId, ids));
    }

    @Delete("DELETE FROM mes_hc_grinding_production_record WHERE id = #{id} AND status = 'WAIT_CONFIRM'")
    int physicalDeleteById(@Param("id") Long id);

    @Delete({"<script>", "DELETE FROM mes_hc_grinding_production_record WHERE status = 'WAIT_CONFIRM' AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>", "</script>"})
    int physicalDeleteByIds(@Param("ids") Collection<Long> ids);
}
