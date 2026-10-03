package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableEventPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcEquipmentConsumableEventMapper extends BaseMapperX<HcEquipmentConsumableEventDO> {

    default PageResult<HcEquipmentConsumableEventDO> selectPage(HcEquipmentConsumableEventPageReqVO reqVO) {
        LambdaQueryWrapperX<HcEquipmentConsumableEventDO> wrapper = new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                .eqIfPresent(HcEquipmentConsumableEventDO::getStateId, reqVO.getStateId())
                .eqIfPresent(HcEquipmentConsumableEventDO::getGuideClothRecordId, reqVO.getGuideClothRecordId())
                .eqIfPresent(HcEquipmentConsumableEventDO::getEquipmentId, reqVO.getEquipmentId())
                .likeIfPresent(HcEquipmentConsumableEventDO::getEquipmentCode, reqVO.getEquipmentCode())
                .likeIfPresent(HcEquipmentConsumableEventDO::getProcessCode, reqVO.getProcessCode())
                .eqIfPresent(HcEquipmentConsumableEventDO::getConsumableType, reqVO.getConsumableType())
                .eqIfPresent(HcEquipmentConsumableEventDO::getEventType, reqVO.getEventType())
                .likeIfPresent(HcEquipmentConsumableEventDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcEquipmentConsumableEventDO::getOperatorName, reqVO.getOperatorName());
        if (reqVO.getBatchNo() != null && !reqVO.getBatchNo().isBlank()) {
            wrapper.and(item -> item
                    .like(HcEquipmentConsumableEventDO::getBeforeBatchNo, reqVO.getBatchNo())
                    .or()
                    .like(HcEquipmentConsumableEventDO::getAfterBatchNo, reqVO.getBatchNo()));
        }
        return selectPage(reqVO, wrapper
                .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                .orderByDesc(HcEquipmentConsumableEventDO::getEventTime)
                .orderByDesc(HcEquipmentConsumableEventDO::getId));
    }

    default List<HcEquipmentConsumableEventDO> selectRecentByStateId(Long stateId, Integer limit) {
        return selectList(new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                .eq(HcEquipmentConsumableEventDO::getStateId, stateId)
                .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                .orderByDesc(HcEquipmentConsumableEventDO::getEventTime)
                .orderByDesc(HcEquipmentConsumableEventDO::getId)
                .last("LIMIT " + (limit == null || limit <= 0 ? 20 : Math.min(limit, 100))));
    }

    /**
     * 同步保护不能只取最后一条审计：查询来源完工后的实际事件，以及有来源关联的同步事件。
     * 同步事件还需回查来源完工时间，不能拿审计时间当作业务时间；不设条数上限。
     */
    default List<HcEquipmentConsumableEventDO> selectConsumableSyncCandidates(
            Long stateId, Long tenantId, LocalDateTime recordTime) {
        return selectList(new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                .eq(HcEquipmentConsumableEventDO::getStateId, stateId)
                .eq(HcEquipmentConsumableEventDO::getTenantId, tenantId)
                .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                .and(wrapper -> wrapper
                        .ge(HcEquipmentConsumableEventDO::getEventTime, recordTime)
                        .or().isNull(HcEquipmentConsumableEventDO::getEventTime)
                        .or(nested -> nested
                                .eq(HcEquipmentConsumableEventDO::getEventType, "ADJUST")
                                .in(HcEquipmentConsumableEventDO::getBizType,
                                        "GRINDING_PRODUCTION_RECORD_CONSUMABLE_SYNC",
                                        "GRINDING_PRODUCTION_RECORD_CONFIRM")))
                .orderByAsc(HcEquipmentConsumableEventDO::getId));
    }

    default HcEquipmentConsumableEventDO selectLatestByStateIdAndEventType(Long stateId, String eventType) {
        return selectOne(new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                .eq(HcEquipmentConsumableEventDO::getStateId, stateId)
                .eq(HcEquipmentConsumableEventDO::getEventType, eventType)
                .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                .orderByDesc(HcEquipmentConsumableEventDO::getEventTime)
                .orderByDesc(HcEquipmentConsumableEventDO::getId)
                .last("LIMIT 1"));
    }

    default HcEquipmentConsumableEventDO selectLatestByGuideClothRecordId(Long guideClothRecordId,
                                                                            String processCode,
                                                                            String consumableType) {
        if (guideClothRecordId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                .eq(HcEquipmentConsumableEventDO::getGuideClothRecordId, guideClothRecordId)
                .eqIfPresent(HcEquipmentConsumableEventDO::getProcessCode, processCode)
                .eqIfPresent(HcEquipmentConsumableEventDO::getConsumableType, consumableType)
                .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                .orderByDesc(HcEquipmentConsumableEventDO::getEventTime)
                .orderByDesc(HcEquipmentConsumableEventDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcEquipmentConsumableEventDO> selectListByBiz(String bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                .eq(HcEquipmentConsumableEventDO::getBizType, bizType)
                .eq(HcEquipmentConsumableEventDO::getBizId, bizId)
                .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                .orderByAsc(HcEquipmentConsumableEventDO::getEventTime)
                .orderByAsc(HcEquipmentConsumableEventDO::getId));
    }

    default void deleteUsageByBiz(String bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            return;
        }
        delete(new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                .eq(HcEquipmentConsumableEventDO::getBizType, bizType)
                .eq(HcEquipmentConsumableEventDO::getBizId, bizId)
                .in(HcEquipmentConsumableEventDO::getEventType, List.of("USE", "REPLACE")));
    }

    default HcEquipmentConsumableEventDO selectLaterUsageEvent(Long stateId, LocalDateTime eventTime, Long eventId) {
        if (stateId == null || eventTime == null || eventId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                .eq(HcEquipmentConsumableEventDO::getStateId, stateId)
                .in(HcEquipmentConsumableEventDO::getEventType, List.of("USE", "REPLACE"))
                .and(wrapper -> wrapper
                        .gt(HcEquipmentConsumableEventDO::getEventTime, eventTime)
                        .or(nested -> nested
                                .eq(HcEquipmentConsumableEventDO::getEventTime, eventTime)
                                .gt(HcEquipmentConsumableEventDO::getId, eventId)))
                .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                .orderByAsc(HcEquipmentConsumableEventDO::getEventTime)
                .orderByAsc(HcEquipmentConsumableEventDO::getId)
                .last("LIMIT 1"));
    }

    default HcEquipmentConsumableEventDO selectLatestBeforeByStateIdAndEventType(Long stateId, String eventType,
                                                                                 LocalDateTime eventTime, Long eventId) {
        if (stateId == null || eventType == null || eventTime == null || eventId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                .eq(HcEquipmentConsumableEventDO::getStateId, stateId)
                .eq(HcEquipmentConsumableEventDO::getEventType, eventType)
                .and(wrapper -> wrapper
                        .lt(HcEquipmentConsumableEventDO::getEventTime, eventTime)
                        .or(nested -> nested
                                .eq(HcEquipmentConsumableEventDO::getEventTime, eventTime)
                                .lt(HcEquipmentConsumableEventDO::getId, eventId)))
                .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                .orderByDesc(HcEquipmentConsumableEventDO::getEventTime)
                .orderByDesc(HcEquipmentConsumableEventDO::getId)
                .last("LIMIT 1"));
    }

    /** 锁定事件后再次检查归属，避免不同报工同时领取同一条看板更换原因。 */
    default HcEquipmentConsumableEventDO selectByIdForUpdate(Long id, Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<HcEquipmentConsumableEventDO>()
                .eq(HcEquipmentConsumableEventDO::getId, id)
                .eq(HcEquipmentConsumableEventDO::getTenantId, tenantId)
                .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default int bindStandaloneReplacement(Long id, Long tenantId, String bizType, Long recordId) {
        return update(null, new LambdaUpdateWrapper<HcEquipmentConsumableEventDO>()
                .eq(HcEquipmentConsumableEventDO::getId, id)
                .eq(HcEquipmentConsumableEventDO::getTenantId, tenantId)
                .eq(HcEquipmentConsumableEventDO::getDeleted, false)
                .eq(HcEquipmentConsumableEventDO::getEventType, "REPLACE")
                .and(w -> w.isNull(HcEquipmentConsumableEventDO::getBizType)
                        .or().eq(HcEquipmentConsumableEventDO::getBizType, ""))
                .isNull(HcEquipmentConsumableEventDO::getBizId)
                .and(w -> w.isNull(HcEquipmentConsumableEventDO::getGrindingStage)
                        .or().eq(HcEquipmentConsumableEventDO::getGrindingStage, ""))
                .isNull(HcEquipmentConsumableEventDO::getGrindingDetailId)
                .set(HcEquipmentConsumableEventDO::getBizType, bizType)
                .set(HcEquipmentConsumableEventDO::getBizId, recordId));
    }

    @Delete("DELETE FROM mes_sfc_equipment_consumable_event WHERE tenant_id = #{tenantId}")
    int physicalDeleteByTenantId(@Param("tenantId") Long tenantId);
}
