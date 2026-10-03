package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo.HcEquipmentConsumableStatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableStateDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcEquipmentConsumableStateMapper extends BaseMapperX<HcEquipmentConsumableStateDO> {

    default PageResult<HcEquipmentConsumableStateDO> selectPage(HcEquipmentConsumableStatePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcEquipmentConsumableStateDO> selectList(HcEquipmentConsumableStatePageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcEquipmentConsumableStateDO> buildQuery(HcEquipmentConsumableStatePageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcEquipmentConsumableStateDO>()
                .likeIfPresent(HcEquipmentConsumableStateDO::getEquipmentCode, reqVO.getEquipmentCode())
                .likeIfPresent(HcEquipmentConsumableStateDO::getEquipmentName, reqVO.getEquipmentName())
                .likeIfPresent(HcEquipmentConsumableStateDO::getProcessCode, reqVO.getProcessCode())
                .likeIfPresent(HcEquipmentConsumableStateDO::getProcessName, reqVO.getProcessName())
                .eqIfPresent(HcEquipmentConsumableStateDO::getConsumableType, reqVO.getConsumableType())
                .likeIfPresent(HcEquipmentConsumableStateDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(HcEquipmentConsumableStateDO::getWarningFlag, reqVO.getWarningFlag())
                .eqIfPresent(HcEquipmentConsumableStateDO::getStatus, reqVO.getStatus())
                .eq(HcEquipmentConsumableStateDO::getDeleted, false)
                .orderByDesc(HcEquipmentConsumableStateDO::getWarningFlag)
                .orderByDesc(HcEquipmentConsumableStateDO::getLastEventTime)
                .orderByDesc(HcEquipmentConsumableStateDO::getId);
    }

    default List<HcEquipmentConsumableStateDO> selectListByEquipment(Long equipmentId, String processCode) {
        return selectList(new LambdaQueryWrapperX<HcEquipmentConsumableStateDO>()
                .eq(HcEquipmentConsumableStateDO::getEquipmentId, equipmentId)
                .eqIfPresent(HcEquipmentConsumableStateDO::getProcessCode, processCode)
                .eq(HcEquipmentConsumableStateDO::getDeleted, false)
                .orderByAsc(HcEquipmentConsumableStateDO::getConsumableType)
                .orderByDesc(HcEquipmentConsumableStateDO::getId));
    }

    default HcEquipmentConsumableStateDO selectOneByEquipmentAndType(Long equipmentId, String processCode, String consumableType) {
        return selectOne(new LambdaQueryWrapperX<HcEquipmentConsumableStateDO>()
                .eq(HcEquipmentConsumableStateDO::getEquipmentId, equipmentId)
                .eqIfPresent(HcEquipmentConsumableStateDO::getProcessCode, processCode)
                .eq(HcEquipmentConsumableStateDO::getConsumableType, consumableType)
                .eq(HcEquipmentConsumableStateDO::getDeleted, false)
                .orderByDesc(HcEquipmentConsumableStateDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcEquipmentConsumableStateDO> selectListByBatchNoAndType(String batchNo, String processCode,
                                                                            String consumableType) {
        if (batchNo == null || batchNo.isBlank()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcEquipmentConsumableStateDO>()
                .eq(HcEquipmentConsumableStateDO::getBatchNo, batchNo)
                .eqIfPresent(HcEquipmentConsumableStateDO::getProcessCode, processCode)
                .eq(HcEquipmentConsumableStateDO::getConsumableType, consumableType)
                .eq(HcEquipmentConsumableStateDO::getDeleted, false)
                .orderByDesc(HcEquipmentConsumableStateDO::getLastEventTime)
                .orderByDesc(HcEquipmentConsumableStateDO::getId));
    }

    @Delete("DELETE FROM mes_sfc_equipment_consumable_state WHERE tenant_id = #{tenantId}")
    int physicalDeleteByTenantId(@Param("tenantId") Long tenantId);
}
