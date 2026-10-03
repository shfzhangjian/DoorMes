package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingMiddleProductRecordDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.util.StringUtils;

@Mapper
public interface HcGrindingMiddleProductRecordMapper extends BaseMapperX<HcGrindingMiddleProductRecordDO> {

    default PageResult<HcGrindingMiddleProductRecordDO> selectPage(HcStationRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcGrindingMiddleProductRecordDO> wrapper = new LambdaQueryWrapperX<HcGrindingMiddleProductRecordDO>()
                .likeIfPresent(HcGrindingMiddleProductRecordDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcGrindingMiddleProductRecordDO::getOperationName, reqVO.getOperationName())
                .likeIfPresent(HcGrindingMiddleProductRecordDO::getFormName, reqVO.getFormName())
                .eqIfPresent(HcGrindingMiddleProductRecordDO::getFormCode, reqVO.getFormCode())
                .likeIfPresent(HcGrindingMiddleProductRecordDO::getEquipmentName, reqVO.getEquipmentName())
                .likeIfPresent(HcGrindingMiddleProductRecordDO::getEquipmentCode, reqVO.getEquipmentCode())
                .likeIfPresent(HcGrindingMiddleProductRecordDO::getConfirmerName, reqVO.getConfirmUserName())
                .geIfPresent(HcGrindingMiddleProductRecordDO::getCreateTime, reqVO.getCreateTimeStart())
                .leIfPresent(HcGrindingMiddleProductRecordDO::getCreateTime, reqVO.getCreateTimeEnd())
                .geIfPresent(HcGrindingMiddleProductRecordDO::getConfirmerTime, reqVO.getConfirmTimeStart())
                .leIfPresent(HcGrindingMiddleProductRecordDO::getConfirmerTime, reqVO.getConfirmTimeEnd());
        if (StringUtils.hasText(reqVO.getCreateUserName())) {
            String createUserName = reqVO.getCreateUserName().trim();
            wrapper.and(query -> query
                    .like(HcGrindingMiddleProductRecordDO::getRecorderName, createUserName)
                    .or().like(HcGrindingMiddleProductRecordDO::getCreator, createUserName));
        }
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(query -> query
                    .like(HcGrindingMiddleProductRecordDO::getPlanNo, keyword)
                    .or().like(HcGrindingMiddleProductRecordDO::getOperationName, keyword)
                    .or().like(HcGrindingMiddleProductRecordDO::getFormName, keyword)
                    .or().like(HcGrindingMiddleProductRecordDO::getFormCode, keyword)
                    .or().like(HcGrindingMiddleProductRecordDO::getEquipmentName, keyword)
                    .or().like(HcGrindingMiddleProductRecordDO::getEquipmentCode, keyword)
                    .or().like(HcGrindingMiddleProductRecordDO::getProductionBatchNo, keyword)
                    .or().like(HcGrindingMiddleProductRecordDO::getMotherModelCode, keyword)
                    .or().like(HcGrindingMiddleProductRecordDO::getMotherModelName, keyword)
                    .or().like(HcGrindingMiddleProductRecordDO::getMaterialCode, keyword));
        }
        wrapper.eq(HcGrindingMiddleProductRecordDO::getDeleted, false)
                .orderByDesc(HcGrindingMiddleProductRecordDO::getCreateTime)
                .orderByDesc(HcGrindingMiddleProductRecordDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default HcGrindingMiddleProductRecordDO selectByBiz(String bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcGrindingMiddleProductRecordDO>()
                .eq(HcGrindingMiddleProductRecordDO::getBizType, bizType)
                .eq(HcGrindingMiddleProductRecordDO::getBizId, bizId)
                .eq(HcGrindingMiddleProductRecordDO::getDeleted, false)
                .orderByDesc(HcGrindingMiddleProductRecordDO::getId)
                .last("LIMIT 1"));
    }

    default HcGrindingMiddleProductRecordDO selectBySegment(Long planOperationId, String passType, String segmentMark) {
        if (planOperationId == null || passType == null || segmentMark == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcGrindingMiddleProductRecordDO>()
                .eq(HcGrindingMiddleProductRecordDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingMiddleProductRecordDO::getPassType, passType)
                .eq(HcGrindingMiddleProductRecordDO::getSegmentMark, segmentMark)
                .eq(HcGrindingMiddleProductRecordDO::getDeleted, false)
                .orderByDesc(HcGrindingMiddleProductRecordDO::getId)
                .last("LIMIT 1"));
    }

    default HcGrindingMiddleProductRecordDO selectByProductionBatchNo(Long planOperationId, String passType, String productionBatchNo) {
        if (planOperationId == null || passType == null || productionBatchNo == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcGrindingMiddleProductRecordDO>()
                .eq(HcGrindingMiddleProductRecordDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingMiddleProductRecordDO::getPassType, passType)
                .eq(HcGrindingMiddleProductRecordDO::getProductionBatchNo, productionBatchNo)
                .eq(HcGrindingMiddleProductRecordDO::getDeleted, false)
                .orderByDesc(HcGrindingMiddleProductRecordDO::getId)
                .last("LIMIT 1"));
    }

    @Delete("DELETE FROM mes_sfc_grinding_middle_product_record WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
