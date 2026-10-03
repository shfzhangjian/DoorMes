package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo.HcCutRoundSpareRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareRecordDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcCutRoundSpareRecordMapper extends BaseMapperX<HcCutRoundSpareRecordDO> {

    default PageResult<HcCutRoundSpareRecordDO> selectPage(HcCutRoundSpareRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcCutRoundSpareRecordDO> wrapper = new LambdaQueryWrapperX<HcCutRoundSpareRecordDO>()
                .eqIfPresent(HcCutRoundSpareRecordDO::getSpareId, reqVO.getSpareId())
                .eqIfPresent(HcCutRoundSpareRecordDO::getEquipmentId, reqVO.getEquipmentId())
                .likeIfPresent(HcCutRoundSpareRecordDO::getEquipmentCode, reqVO.getEquipmentCode())
                .eqIfPresent(HcCutRoundSpareRecordDO::getSpareType, reqVO.getSpareType())
                .eqIfPresent(HcCutRoundSpareRecordDO::getEventType, reqVO.getEventType())
                .likeIfPresent(HcCutRoundSpareRecordDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcCutRoundSpareRecordDO::getOperatorName, reqVO.getOperatorName());
        if (reqVO.getBatchNo() != null && !reqVO.getBatchNo().isBlank()) {
            wrapper.and(item -> item
                    .like(HcCutRoundSpareRecordDO::getBeforeBatchNo, reqVO.getBatchNo())
                    .or()
                    .like(HcCutRoundSpareRecordDO::getAfterBatchNo, reqVO.getBatchNo()));
        }
        return selectPage(reqVO, wrapper
                .eq(HcCutRoundSpareRecordDO::getDeleted, false)
                .orderByDesc(HcCutRoundSpareRecordDO::getEventTime)
                .orderByDesc(HcCutRoundSpareRecordDO::getId));
    }

    default HcCutRoundSpareRecordDO byRequest(String requestKey) {
        return selectOne(new LambdaQueryWrapperX<HcCutRoundSpareRecordDO>()
                .eq(HcCutRoundSpareRecordDO::getRequestKey, requestKey).last("LIMIT 1 FOR UPDATE"));
    }

    default boolean hasInventoryConsumption() {
        return !selectList(new LambdaQueryWrapperX<HcCutRoundSpareRecordDO>()
                .isNotNull(HcCutRoundSpareRecordDO::getConsumeId).last("LIMIT 1 FOR UPDATE")).isEmpty();
    }

    @Delete("DELETE FROM mes_md_cut_round_spare_record WHERE tenant_id = #{tenantId}")
    int physicalDeleteByTenantId(@Param("tenantId") Long tenantId);
}
