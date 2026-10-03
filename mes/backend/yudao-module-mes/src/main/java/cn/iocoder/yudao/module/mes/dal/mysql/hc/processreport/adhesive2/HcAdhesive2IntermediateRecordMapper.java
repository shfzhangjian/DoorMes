package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2IntermediateRecordDO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

@Mapper
public interface HcAdhesive2IntermediateRecordMapper extends BaseMapperX<HcAdhesive2IntermediateRecordDO> {

    default PageResult<HcAdhesive2IntermediateRecordDO> selectPage(HcStationRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcAdhesive2IntermediateRecordDO> wrapper = new LambdaQueryWrapperX<HcAdhesive2IntermediateRecordDO>()
                .likeIfPresent(HcAdhesive2IntermediateRecordDO::getPlanNo, reqVO.getPlanNo())
                .geIfPresent(HcAdhesive2IntermediateRecordDO::getCreateTime, reqVO.getCreateTimeStart())
                .leIfPresent(HcAdhesive2IntermediateRecordDO::getCreateTime, reqVO.getCreateTimeEnd());
        if (StringUtils.hasText(reqVO.getCreateUserName())) {
            String createUserName = reqVO.getCreateUserName().trim();
            wrapper.and(query -> query
                    .like(HcAdhesive2IntermediateRecordDO::getRecorderName, createUserName)
                    .or().like(HcAdhesive2IntermediateRecordDO::getCreator, createUserName));
        }
        if (StringUtils.hasText(reqVO.getConfirmUserName())) {
            wrapper.like(HcAdhesive2IntermediateRecordDO::getConfirmerName, reqVO.getConfirmUserName().trim());
        }
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(query -> query
                    .like(HcAdhesive2IntermediateRecordDO::getPlanNo, keyword)
                    .or().like(HcAdhesive2IntermediateRecordDO::getBatchNo, keyword)
                    .or().like(HcAdhesive2IntermediateRecordDO::getModelCode, keyword)
                    .or().like(HcAdhesive2IntermediateRecordDO::getMaterialCode, keyword)
                    .or().like(HcAdhesive2IntermediateRecordDO::getGlueBoardModel, keyword)
                    .or().like(HcAdhesive2IntermediateRecordDO::getRecorderName, keyword)
                    .or().like(HcAdhesive2IntermediateRecordDO::getConfirmerName, keyword));
        }
        wrapper.eq(HcAdhesive2IntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcAdhesive2IntermediateRecordDO::getRecordDate)
                .orderByDesc(HcAdhesive2IntermediateRecordDO::getUpdateTime)
                .orderByDesc(HcAdhesive2IntermediateRecordDO::getCreateTime)
                .orderByDesc(HcAdhesive2IntermediateRecordDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default HcAdhesive2IntermediateRecordDO selectByPlanOperationIdAndDate(Long planOperationId, LocalDate recordDate) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesive2IntermediateRecordDO>()
                .eq(HcAdhesive2IntermediateRecordDO::getPlanOperationId, planOperationId)
                .ge(HcAdhesive2IntermediateRecordDO::getRecordDate, recordDate.atStartOfDay())
                .lt(HcAdhesive2IntermediateRecordDO::getRecordDate, recordDate.plusDays(1).atStartOfDay())
                .eq(HcAdhesive2IntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcAdhesive2IntermediateRecordDO::getId)
                .last("LIMIT 1"));
    }

    default HcAdhesive2IntermediateRecordDO selectByPlanOperationIdAndBatchNo(Long planOperationId, String batchNo) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesive2IntermediateRecordDO>()
                .eq(HcAdhesive2IntermediateRecordDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesive2IntermediateRecordDO::getBatchNo, batchNo)
                .eq(HcAdhesive2IntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcAdhesive2IntermediateRecordDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcAdhesive2IntermediateRecordDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesive2IntermediateRecordDO>()
                .eq(HcAdhesive2IntermediateRecordDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesive2IntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcAdhesive2IntermediateRecordDO::getRecordDate)
                .orderByDesc(HcAdhesive2IntermediateRecordDO::getId));
    }

    default List<HcAdhesive2IntermediateRecordDO> selectListByPlanOperationIdAndBatchNo(Long planOperationId, String batchNo) {
        return selectList(new LambdaQueryWrapperX<HcAdhesive2IntermediateRecordDO>()
                .eq(HcAdhesive2IntermediateRecordDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesive2IntermediateRecordDO::getBatchNo, batchNo)
                .eq(HcAdhesive2IntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcAdhesive2IntermediateRecordDO::getRecordDate)
                .orderByDesc(HcAdhesive2IntermediateRecordDO::getId));
    }
}
