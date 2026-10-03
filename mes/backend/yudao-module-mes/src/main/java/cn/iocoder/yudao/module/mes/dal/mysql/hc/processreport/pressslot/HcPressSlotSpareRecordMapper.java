package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo.HcPressSlotSpareRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareRecordDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcPressSlotSpareRecordMapper extends BaseMapperX<HcPressSlotSpareRecordDO> {

    default PageResult<HcPressSlotSpareRecordDO> selectPage(HcPressSlotSpareRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcPressSlotSpareRecordDO> wrapper = new LambdaQueryWrapperX<HcPressSlotSpareRecordDO>()
                .eqIfPresent(HcPressSlotSpareRecordDO::getSpareId, reqVO.getSpareId())
                .eqIfPresent(HcPressSlotSpareRecordDO::getEquipmentId, reqVO.getEquipmentId())
                .likeIfPresent(HcPressSlotSpareRecordDO::getEquipmentCode, reqVO.getEquipmentCode())
                .eqIfPresent(HcPressSlotSpareRecordDO::getSpareType, reqVO.getSpareType())
                .eqIfPresent(HcPressSlotSpareRecordDO::getEventType, reqVO.getEventType())
                .likeIfPresent(HcPressSlotSpareRecordDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcPressSlotSpareRecordDO::getOperatorName, reqVO.getOperatorName());
        if (reqVO.getBatchNo() != null && !reqVO.getBatchNo().isBlank()) {
            wrapper.and(item -> item
                    .like(HcPressSlotSpareRecordDO::getBeforeBatchNo, reqVO.getBatchNo())
                    .or()
                    .like(HcPressSlotSpareRecordDO::getAfterBatchNo, reqVO.getBatchNo()));
        }
        return selectPage(reqVO, wrapper
                .eq(HcPressSlotSpareRecordDO::getDeleted, false)
                .orderByDesc(HcPressSlotSpareRecordDO::getEventTime)
                .orderByDesc(HcPressSlotSpareRecordDO::getId));
    }

    default HcPressSlotSpareRecordDO selectLatestBySpareIdAndEventType(Long spareId, String eventType) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotSpareRecordDO>()
                .eq(HcPressSlotSpareRecordDO::getSpareId, spareId)
                .eq(HcPressSlotSpareRecordDO::getEventType, eventType)
                .eq(HcPressSlotSpareRecordDO::getDeleted, false)
                .orderByDesc(HcPressSlotSpareRecordDO::getEventTime)
                .orderByDesc(HcPressSlotSpareRecordDO::getId)
                .last("LIMIT 1"));
    }

    @Delete("DELETE FROM mes_md_press_slot_spare_record WHERE tenant_id = #{tenantId}")
    int physicalDeleteByTenantId(@Param("tenantId") Long tenantId);
}
