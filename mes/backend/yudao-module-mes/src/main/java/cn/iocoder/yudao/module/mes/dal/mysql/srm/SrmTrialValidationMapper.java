package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmTrialValidationPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmTrialValidationDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SrmTrialValidationMapper extends BaseMapperX<SrmTrialValidationDO> {

    default PageResult<SrmTrialValidationDO> selectPage(SrmTrialValidationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmTrialValidationDO>()
                .likeIfPresent(SrmTrialValidationDO::getTrialNo, reqVO.getTrialNo())
                .likeIfPresent(SrmTrialValidationDO::getSourceSampleEvaluationNo, reqVO.getSourceSampleEvaluationNo())
                .likeIfPresent(SrmTrialValidationDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(SrmTrialValidationDO::getSupplierName, reqVO.getSupplierName())
                .likeIfPresent(SrmTrialValidationDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(SrmTrialValidationDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(SrmTrialValidationDO::getMaterialModel, reqVO.getMaterialModel())
                .eqIfPresent(SrmTrialValidationDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SrmTrialValidationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SrmTrialValidationDO::getUpdateTime)
                .orderByDesc(SrmTrialValidationDO::getId));
    }

    default SrmTrialValidationDO selectByTrialNo(String trialNo) {
        return selectOne(SrmTrialValidationDO::getTrialNo, trialNo);
    }

    default SrmTrialValidationDO selectBySourceSampleEvaluationId(Long sourceSampleEvaluationId) {
        return selectOne(new LambdaQueryWrapperX<SrmTrialValidationDO>()
                .eq(SrmTrialValidationDO::getSourceSampleEvaluationId, sourceSampleEvaluationId)
                .orderByDesc(SrmTrialValidationDO::getId)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT trial.*
              FROM mes_srm_trial_validation trial
              JOIN mes_srm_sample_evaluation evaluation
                ON evaluation.id = trial.source_sample_evaluation_id
               AND evaluation.deleted = b'0'
             WHERE trial.deleted = b'0'
               AND evaluation.sample_request_id = #{sampleRequestId}
             ORDER BY trial.update_time DESC, trial.id DESC
            """)
    List<SrmTrialValidationDO> selectListBySampleRequestId(@Param("sampleRequestId") Long sampleRequestId);

}
