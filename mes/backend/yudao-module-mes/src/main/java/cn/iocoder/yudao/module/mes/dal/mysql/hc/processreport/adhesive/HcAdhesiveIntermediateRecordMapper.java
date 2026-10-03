package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveIntermediateRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

@Mapper
public interface HcAdhesiveIntermediateRecordMapper extends BaseMapperX<HcAdhesiveIntermediateRecordDO> {

    default PageResult<HcAdhesiveIntermediateRecordDO> selectPage(HcStationRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcAdhesiveIntermediateRecordDO> wrapper = new LambdaQueryWrapperX<HcAdhesiveIntermediateRecordDO>()
                .likeIfPresent(HcAdhesiveIntermediateRecordDO::getPlanNo, reqVO.getPlanNo())
                .geIfPresent(HcAdhesiveIntermediateRecordDO::getCreateTime, reqVO.getCreateTimeStart())
                .leIfPresent(HcAdhesiveIntermediateRecordDO::getCreateTime, reqVO.getCreateTimeEnd());
        if (StringUtils.hasText(reqVO.getCreateUserName())) {
            String createUserName = reqVO.getCreateUserName().trim();
            wrapper.and(query -> query
                    .like(HcAdhesiveIntermediateRecordDO::getRecorderName, createUserName)
                    .or().like(HcAdhesiveIntermediateRecordDO::getCreator, createUserName));
        }
        if (StringUtils.hasText(reqVO.getConfirmUserName())) {
            wrapper.like(HcAdhesiveIntermediateRecordDO::getConfirmerName, reqVO.getConfirmUserName().trim());
        }
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(query -> query
                    .like(HcAdhesiveIntermediateRecordDO::getPlanNo, keyword)
                    .or().like(HcAdhesiveIntermediateRecordDO::getBatchNo, keyword)
                    .or().like(HcAdhesiveIntermediateRecordDO::getModelCode, keyword)
                    .or().like(HcAdhesiveIntermediateRecordDO::getMaterialCode, keyword)
                    .or().like(HcAdhesiveIntermediateRecordDO::getRecorderName, keyword)
                    .or().like(HcAdhesiveIntermediateRecordDO::getConfirmerName, keyword));
        }
        wrapper.eq(HcAdhesiveIntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcAdhesiveIntermediateRecordDO::getRecordTime)
                .orderByDesc(HcAdhesiveIntermediateRecordDO::getCreateTime)
                .orderByDesc(HcAdhesiveIntermediateRecordDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default HcAdhesiveIntermediateRecordDO selectByPlanOperationId(Long planOperationId) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveIntermediateRecordDO>()
                .eq(HcAdhesiveIntermediateRecordDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesiveIntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcAdhesiveIntermediateRecordDO::getId)
                .last("LIMIT 1"));
    }

    default HcAdhesiveIntermediateRecordDO selectByAdhesiveReportId(Long adhesiveReportId) {
        if (adhesiveReportId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveIntermediateRecordDO>()
                .eq(HcAdhesiveIntermediateRecordDO::getAdhesiveReportId, adhesiveReportId)
                .eq(HcAdhesiveIntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcAdhesiveIntermediateRecordDO::getId)
                .last("LIMIT 1"));
    }

    default HcAdhesiveIntermediateRecordDO selectByPlanOperationIdAndBatchNo(Long planOperationId, String batchNo) {
        if (planOperationId == null || batchNo == null || batchNo.trim().isEmpty()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveIntermediateRecordDO>()
                .eq(HcAdhesiveIntermediateRecordDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesiveIntermediateRecordDO::getBatchNo, batchNo.trim())
                .eq(HcAdhesiveIntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcAdhesiveIntermediateRecordDO::getId)
                .last("LIMIT 1"));
    }
}
