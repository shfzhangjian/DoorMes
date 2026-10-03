package cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HcEquipmentMapper extends BaseMapperX<HcEquipmentDO> {

    @Update("""
            UPDATE mes_md_equipment
            SET work_status = 'PRODUCING',
                current_plan_no = #{planNo},
                current_operation_code = #{operationCode},
                current_operation_name = #{operationName},
                current_start_time = #{startTime},
                current_end_time = NULL,
                current_operator_name = #{operatorName},
                current_record_time = #{recordTime},
                update_time = NOW()
            WHERE id = #{equipmentId}
              AND deleted = 0
              AND status = 0
              AND (
                current_plan_no = #{planNo}
                OR work_status IS NULL
                OR work_status = ''
                OR work_status IN ('IDLE', 'FREE', 'AVAILABLE')
              )
            """)
    int tryOccupyForPlan(@Param("equipmentId") Long equipmentId,
                         @Param("planNo") String planNo,
                         @Param("operationCode") String operationCode,
                         @Param("operationName") String operationName,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("operatorName") String operatorName,
                         @Param("recordTime") LocalDateTime recordTime);

    default PageResult<HcEquipmentDO> selectPage(HcEquipmentPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcEquipmentDO> selectList(HcEquipmentPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcEquipmentDO> buildQuery(HcEquipmentPageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcEquipmentDO>()
                .likeIfPresent(HcEquipmentDO::getEquipmentCode, reqVO.getEquipmentCode())
                .likeIfPresent(HcEquipmentDO::getEquipmentName, reqVO.getEquipmentName())
                .eqIfPresent(HcEquipmentDO::getWorkCenterId, reqVO.getWorkCenterId())
                .likeIfPresent(HcEquipmentDO::getWorkCenterCode, reqVO.getWorkCenterCode())
                .likeIfPresent(HcEquipmentDO::getWorkCenterName, reqVO.getWorkCenterName())
                .eqIfPresent(HcEquipmentDO::getEquipmentType, reqVO.getEquipmentType())
                .eqIfPresent(HcEquipmentDO::getApplicablePadType, reqVO.getApplicablePadType())
                .eqIfPresent(HcEquipmentDO::getAssetNo, reqVO.getAssetNo())
                .eqIfPresent(HcEquipmentDO::getEnableQcChecklist, reqVO.getEnableQcChecklist())
                .eqIfPresent(HcEquipmentDO::getEnableCleanChecklist, reqVO.getEnableCleanChecklist())
                .eqIfPresent(HcEquipmentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HcEquipmentDO::getWorkStatus, reqVO.getWorkStatus())
                .likeIfPresent(HcEquipmentDO::getCurrentPlanNo, reqVO.getCurrentPlanNo())
                .likeIfPresent(HcEquipmentDO::getCurrentOperationCode, reqVO.getCurrentOperationCode())
                .likeIfPresent(HcEquipmentDO::getCurrentOperationName, reqVO.getCurrentOperationName())
                .eqIfPresent(HcEquipmentDO::getRemark, reqVO.getRemark())
                .orderByDesc(HcEquipmentDO::getId);
    }
}
