package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSparePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcPressSlotSpareMapper extends BaseMapperX<HcPressSlotSpareDO> {

    default PageResult<HcPressSlotSpareDO> selectPage(HcPressSlotSparePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcPressSlotSpareDO> selectList(HcPressSlotSparePageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcPressSlotSpareDO> buildQuery(HcPressSlotSparePageReqVO reqVO) {
        LambdaQueryWrapperX<HcPressSlotSpareDO> wrapper = new LambdaQueryWrapperX<HcPressSlotSpareDO>()
                .likeIfPresent(HcPressSlotSpareDO::getEquipmentCode, reqVO.getEquipmentCode())
                .likeIfPresent(HcPressSlotSpareDO::getEquipmentName, reqVO.getEquipmentName())
                .eqIfPresent(HcPressSlotSpareDO::getSpareType, reqVO.getSpareType())
                .likeIfPresent(HcPressSlotSpareDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(HcPressSlotSpareDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(HcPressSlotSpareDO::getWarningFlag, reqVO.getWarningFlag())
                .eqIfPresent(HcPressSlotSpareDO::getStatus, reqVO.getStatus());
        wrapper.eq(HcPressSlotSpareDO::getDeleted, false)
                .orderByDesc(HcPressSlotSpareDO::getWarningFlag)
                .orderByAsc(HcPressSlotSpareDO::getEquipmentCode)
                .orderByAsc(HcPressSlotSpareDO::getSpareType)
                .orderByDesc(HcPressSlotSpareDO::getId);
        return wrapper;
    }

    default HcPressSlotSpareDO selectOneByEquipmentAndType(Long equipmentId, String spareType) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotSpareDO>()
                .eq(HcPressSlotSpareDO::getEquipmentId, equipmentId)
                .eq(HcPressSlotSpareDO::getSpareType, spareType)
                .eq(HcPressSlotSpareDO::getDeleted, false)
                .orderByDesc(HcPressSlotSpareDO::getId)
                .last("LIMIT 1"));
    }

    @Update({
            "<script>",
            "UPDATE mes_md_press_slot_spare",
            "SET equipment_id = #{equipmentId,jdbcType=BIGINT},",
            "equipment_code = #{equipmentCode,jdbcType=VARCHAR},",
            "equipment_name = #{equipmentName,jdbcType=VARCHAR},",
            "work_center_id = #{workCenterId,jdbcType=BIGINT},",
            "work_center_code = #{workCenterCode,jdbcType=VARCHAR},",
            "work_center_name = #{workCenterName,jdbcType=VARCHAR},",
            "spare_type = #{spareType,jdbcType=VARCHAR},",
            "material_code = #{materialCode,jdbcType=VARCHAR},",
            "material_name = #{materialName,jdbcType=VARCHAR},",
            "batch_no = #{batchNo,jdbcType=VARCHAR},",
            "online_quantity = #{onlineQuantity,jdbcType=DECIMAL},",
            "available_quantity = #{availableQuantity,jdbcType=DECIMAL},",
            "last_replace_time = #{lastReplaceTime,jdbcType=TIMESTAMP},",
            "last_replace_plan_no = #{lastReplacePlanNo,jdbcType=VARCHAR},",
            "last_replace_reason = #{lastReplaceReason,jdbcType=VARCHAR},",
            "last_clean_time = #{lastCleanTime,jdbcType=TIMESTAMP},",
            "last_clean_remark = #{lastCleanRemark,jdbcType=VARCHAR},",
            "use_count = #{useCount,jdbcType=INTEGER},",
            "limit_count = #{limitCount,jdbcType=INTEGER},",
            "limit_days = #{limitDays,jdbcType=INTEGER},",
            "warning_flag = #{warningFlag,jdbcType=INTEGER},",
            "status = #{status,jdbcType=VARCHAR},",
            "last_operator_id = #{lastOperatorId,jdbcType=BIGINT},",
            "last_operator_name = #{lastOperatorName,jdbcType=VARCHAR},",
            "last_event_time = #{lastEventTime,jdbcType=TIMESTAMP},",
            "remark = #{remark,jdbcType=VARCHAR},",
            "tenant_id = #{tenantId,jdbcType=BIGINT},",
            "update_time = NOW()",
            "WHERE id = #{id,jdbcType=BIGINT}",
            "AND tenant_id = #{tenantId,jdbcType=BIGINT}",
            "AND deleted = b'0'",
            "</script>"
    })
    int updateCurrentStateById(HcPressSlotSpareDO state);

    @Delete("DELETE FROM mes_md_press_slot_spare WHERE tenant_id = #{tenantId}")
    int physicalDeleteByTenantId(@Param("tenantId") Long tenantId);
}
