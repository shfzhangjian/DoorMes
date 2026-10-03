package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardUsagePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageDO;
import cn.hutool.core.util.StrUtil;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcAdhesiveGlueBoardUsageMapper extends BaseMapperX<HcAdhesiveGlueBoardUsageDO> {

    @Select("SELECT * FROM mes_sfc_adhesive_glue_board_usage "
            + "WHERE id = #{id} AND deleted = b'0' FOR UPDATE")
    HcAdhesiveGlueBoardUsageDO selectByIdForUpdate(@Param("id") Long id);

    default PageResult<HcAdhesiveGlueBoardUsageDO> selectPage(HcAdhesiveGlueBoardUsagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .likeIfPresent(HcAdhesiveGlueBoardUsageDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcAdhesiveGlueBoardUsageDO::getOperationName, reqVO.getOperationName())
                .likeIfPresent(HcAdhesiveGlueBoardUsageDO::getEquipmentCode, reqVO.getEquipmentCode())
                .likeIfPresent(HcAdhesiveGlueBoardUsageDO::getEquipmentName, reqVO.getEquipmentName())
                .likeIfPresent(HcAdhesiveGlueBoardUsageDO::getGlueBoardMaterialCode, reqVO.getGlueBoardMaterialCode())
                .likeIfPresent(HcAdhesiveGlueBoardUsageDO::getGlueBoardBatchNo, reqVO.getGlueBoardBatchNo())
                .eqIfPresent(HcAdhesiveGlueBoardUsageDO::getUsageStatus, reqVO.getUsageStatus())
                .eqIfPresent(HcAdhesiveGlueBoardUsageDO::getQualityStatus, reqVO.getQualityStatus())
                .eqIfPresent(HcAdhesiveGlueBoardUsageDO::getRecordDate, reqVO.getRecordDate())
                .orderByDesc(HcAdhesiveGlueBoardUsageDO::getRecordDate)
                .orderByDesc(HcAdhesiveGlueBoardUsageDO::getId));
    }

    default List<HcAdhesiveGlueBoardUsageDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .eq(HcAdhesiveGlueBoardUsageDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesiveGlueBoardUsageDO::getDeleted, false)
                .orderByDesc(HcAdhesiveGlueBoardUsageDO::getId));
    }

    default HcAdhesiveGlueBoardUsageDO selectActiveByPlanOperationId(Long planOperationId) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .eq(HcAdhesiveGlueBoardUsageDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesiveGlueBoardUsageDO::getUsageStatus, "ACTIVE")
                .eq(HcAdhesiveGlueBoardUsageDO::getDeleted, false)
                .orderByDesc(HcAdhesiveGlueBoardUsageDO::getId)
                .last("LIMIT 1"));
    }

    default HcAdhesiveGlueBoardUsageDO selectLatestActiveByOperationScope(String processCode, Long equipmentId,
                                                                          String equipmentCode) {
        LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO> wrapper = new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .eq(HcAdhesiveGlueBoardUsageDO::getUsageStatus, "ACTIVE")
                .eq(HcAdhesiveGlueBoardUsageDO::getDeleted, false);
        if ("ADHESIVE2".equalsIgnoreCase(processCode)) {
            wrapper.and(scope -> scope
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "ADHESIVE2")
                    .or()
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "OP-ADHESIVE2")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶2")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "背胶"));
        } else if ("ADHESIVE".equalsIgnoreCase(processCode) || "ADHESIVE1".equalsIgnoreCase(processCode)) {
            wrapper.and(scope -> scope
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "ADHESIVE")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘双面胶")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶1")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶一"));
        }
        if (equipmentId != null && cn.hutool.core.util.StrUtil.isNotBlank(equipmentCode)) {
            wrapper.and(equipment -> equipment
                    .eq(HcAdhesiveGlueBoardUsageDO::getEquipmentId, equipmentId)
                    .or()
                    .eq(HcAdhesiveGlueBoardUsageDO::getEquipmentCode, equipmentCode));
        } else if (equipmentId != null) {
            wrapper.eq(HcAdhesiveGlueBoardUsageDO::getEquipmentId, equipmentId);
        } else {
            wrapper.eqIfPresent(HcAdhesiveGlueBoardUsageDO::getEquipmentCode, equipmentCode);
        }
        return selectOne(wrapper.orderByDesc(HcAdhesiveGlueBoardUsageDO::getId).last("LIMIT 1"));
    }

    default List<HcAdhesiveGlueBoardUsageDO> selectActiveListByOperationScope(String processCode) {
        LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO> wrapper = new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .eq(HcAdhesiveGlueBoardUsageDO::getUsageStatus, "ACTIVE")
                .eq(HcAdhesiveGlueBoardUsageDO::getDeleted, false);
        if ("ADHESIVE2".equalsIgnoreCase(processCode)) {
            wrapper.and(scope -> scope
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "ADHESIVE2")
                    .or()
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "OP-ADHESIVE2")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶2")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "背胶"));
        } else if ("ADHESIVE".equalsIgnoreCase(processCode) || "ADHESIVE1".equalsIgnoreCase(processCode)) {
            wrapper.and(scope -> scope
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "ADHESIVE")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘双面胶")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶1")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶一"));
        }
        return selectList(wrapper.orderByDesc(HcAdhesiveGlueBoardUsageDO::getId));
    }

    /**
     * 粘胶2胶板按机台归属，计划切换后仍可继续使用同机台当前 ACTIVE 胶板。
     */
    default HcAdhesiveGlueBoardUsageDO selectLatestActiveAdhesive2ByEquipment(Long equipmentId,
                                                                               String equipmentCode) {
        LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO> wrapper = buildActiveAdhesive2EquipmentWrapper(
                equipmentId, equipmentCode);
        if (wrapper == null) {
            return null;
        }
        return selectOne(wrapper.orderByDesc(HcAdhesiveGlueBoardUsageDO::getId).last("LIMIT 1"));
    }

    default HcAdhesiveGlueBoardUsageDO selectActiveAdhesive2ByStockAndEquipment(Long glueBoardStockId,
                                                                                 Long equipmentId,
                                                                                 String equipmentCode) {
        if (glueBoardStockId == null) {
            return null;
        }
        LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO> wrapper = buildActiveAdhesive2EquipmentWrapper(
                equipmentId, equipmentCode);
        if (wrapper == null) {
            return null;
        }
        return selectOne(wrapper
                .eq(HcAdhesiveGlueBoardUsageDO::getGlueBoardStockId, glueBoardStockId)
                .orderByDesc(HcAdhesiveGlueBoardUsageDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcAdhesiveGlueBoardUsageDO> selectActiveAdhesive2ListByEquipment(Long equipmentId,
                                                                                   String equipmentCode) {
        LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO> wrapper = buildActiveAdhesive2EquipmentWrapper(
                equipmentId, equipmentCode);
        if (wrapper == null) {
            return Collections.emptyList();
        }
        return selectList(wrapper.orderByDesc(HcAdhesiveGlueBoardUsageDO::getId));
    }

    private LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO> buildActiveAdhesive2EquipmentWrapper(
            Long equipmentId, String equipmentCode) {
        Long effectiveEquipmentId = equipmentId != null && equipmentId > 0 ? equipmentId : null;
        String effectiveEquipmentCode = StrUtil.trimToNull(equipmentCode);
        if (effectiveEquipmentId == null && effectiveEquipmentCode == null) {
            return null;
        }
        LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO> wrapper = new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .eq(HcAdhesiveGlueBoardUsageDO::getUsageStatus, "ACTIVE")
                .eq(HcAdhesiveGlueBoardUsageDO::getDeleted, false);
        wrapper.and(scope -> scope
                .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "ADHESIVE2")
                .or()
                .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "OP-ADHESIVE2")
                .or()
                .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶2")
                .or()
                .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "背胶"));
        if (effectiveEquipmentId != null) {
            if (effectiveEquipmentCode != null) {
                wrapper.and(equipment -> equipment
                        .eq(HcAdhesiveGlueBoardUsageDO::getEquipmentId, effectiveEquipmentId)
                        .or(fallback -> fallback
                                .isNull(HcAdhesiveGlueBoardUsageDO::getEquipmentId)
                                .eq(HcAdhesiveGlueBoardUsageDO::getEquipmentCode, effectiveEquipmentCode)));
            } else {
                wrapper.eq(HcAdhesiveGlueBoardUsageDO::getEquipmentId, effectiveEquipmentId);
            }
        } else {
            wrapper.eq(HcAdhesiveGlueBoardUsageDO::getEquipmentCode, effectiveEquipmentCode);
        }
        return wrapper;
    }

    default boolean existsActiveByGlueBoardStockId(Long glueBoardStockId) {
        if (glueBoardStockId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .eq(HcAdhesiveGlueBoardUsageDO::getGlueBoardStockId, glueBoardStockId)
                .eq(HcAdhesiveGlueBoardUsageDO::getUsageStatus, "ACTIVE")
                .eq(HcAdhesiveGlueBoardUsageDO::getDeleted, false)) > 0;
    }

    default boolean existsActiveByGlueBoardStockIdAndProcessCode(Long glueBoardStockId, String processCode) {
        if (glueBoardStockId == null) {
            return false;
        }
        return selectActiveGlueBoardStockIdsByProcessCode(processCode).contains(glueBoardStockId);
    }

    default boolean existsActiveByGlueBoardBatchNoAndProcessCode(String glueBoardBatchNo, String processCode) {
        if (cn.hutool.core.util.StrUtil.isBlank(glueBoardBatchNo)) {
            return false;
        }
        return selectActiveGlueBoardBatchNosByProcessCode(processCode).contains(cn.hutool.core.util.StrUtil.trim(glueBoardBatchNo));
    }

    default List<Long> selectActiveGlueBoardStockIdsByProcessCode(String processCode) {
        LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO> wrapper = new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .eq(HcAdhesiveGlueBoardUsageDO::getUsageStatus, "ACTIVE")
                .eq(HcAdhesiveGlueBoardUsageDO::getDeleted, false);
        if ("ADHESIVE2".equalsIgnoreCase(processCode)) {
            wrapper.and(scope -> scope
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "ADHESIVE2")
                    .or()
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "OP-ADHESIVE2")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶2")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "背胶"));
        } else if ("ADHESIVE".equalsIgnoreCase(processCode) || "ADHESIVE1".equalsIgnoreCase(processCode)) {
            wrapper.and(scope -> scope
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "ADHESIVE")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘双面胶")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶1")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶一"));
        }
        return selectList(wrapper).stream()
                .map(HcAdhesiveGlueBoardUsageDO::getGlueBoardStockId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    default List<String> selectActiveGlueBoardBatchNosByProcessCode(String processCode) {
        LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO> wrapper = new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .eq(HcAdhesiveGlueBoardUsageDO::getUsageStatus, "ACTIVE")
                .eq(HcAdhesiveGlueBoardUsageDO::getDeleted, false);
        if ("ADHESIVE2".equalsIgnoreCase(processCode)) {
            wrapper.and(scope -> scope
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "ADHESIVE2")
                    .or()
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "OP-ADHESIVE2")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶2")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "背胶"));
        } else if ("ADHESIVE".equalsIgnoreCase(processCode) || "ADHESIVE1".equalsIgnoreCase(processCode)) {
            wrapper.and(scope -> scope
                    .eq(HcAdhesiveGlueBoardUsageDO::getOperationCode, "ADHESIVE")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘双面胶")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶1")
                    .or()
                    .like(HcAdhesiveGlueBoardUsageDO::getOperationName, "粘胶一"));
        }
        return selectList(wrapper).stream()
                .map(HcAdhesiveGlueBoardUsageDO::getGlueBoardBatchNo)
                .filter(cn.hutool.core.util.StrUtil::isNotBlank)
                .map(cn.hutool.core.util.StrUtil::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    default List<HcAdhesiveGlueBoardUsageDO> selectAbnormalByBatchNo(String glueBoardBatchNo) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .eq(HcAdhesiveGlueBoardUsageDO::getGlueBoardBatchNo, glueBoardBatchNo)
                .eq(HcAdhesiveGlueBoardUsageDO::getQualityStatus, "ABNORMAL")
                .eq(HcAdhesiveGlueBoardUsageDO::getDeleted, false)
                .orderByAsc(HcAdhesiveGlueBoardUsageDO::getQualityLockStartPosition));
    }

    default List<HcAdhesiveGlueBoardUsageDO> selectListByGlueBoardStockId(Long glueBoardStockId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .eq(HcAdhesiveGlueBoardUsageDO::getGlueBoardStockId, glueBoardStockId)
                .eq(HcAdhesiveGlueBoardUsageDO::getDeleted, false)
                .orderByAsc(HcAdhesiveGlueBoardUsageDO::getId));
    }

    default List<HcAdhesiveGlueBoardUsageDO> selectListByGlueBoardBatchNo(String glueBoardBatchNo) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveGlueBoardUsageDO>()
                .eq(HcAdhesiveGlueBoardUsageDO::getGlueBoardBatchNo, glueBoardBatchNo)
                .eq(HcAdhesiveGlueBoardUsageDO::getDeleted, false)
                .orderByAsc(HcAdhesiveGlueBoardUsageDO::getId));
    }
}
