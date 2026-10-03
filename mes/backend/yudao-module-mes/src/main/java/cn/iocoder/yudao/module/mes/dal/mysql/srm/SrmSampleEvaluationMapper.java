package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSampleEvaluationMapper extends BaseMapperX<SrmSampleEvaluationDO> {

    default PageResult<SrmSampleEvaluationDO> selectPage(SrmSampleEvaluationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmSampleEvaluationDO>()
                .likeIfPresent(SrmSampleEvaluationDO::getEvaluationNo, reqVO.getEvaluationNo())
                .likeIfPresent(SrmSampleEvaluationDO::getSampleRequestNo, reqVO.getSampleRequestNo())
                .eqIfPresent(SrmSampleEvaluationDO::getProjectId, reqVO.getProjectId())
                .likeIfPresent(SrmSampleEvaluationDO::getProjectName, reqVO.getProjectName())
                .likeIfPresent(SrmSampleEvaluationDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(SrmSampleEvaluationDO::getSupplierName, reqVO.getSupplierName())
                .likeIfPresent(SrmSampleEvaluationDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(SrmSampleEvaluationDO::getMaterialModel, reqVO.getMaterialModel())
                .eqIfPresent(SrmSampleEvaluationDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SrmSampleEvaluationDO::getEvaluationDate, reqVO.getEvaluationDate())
                .betweenIfPresent(SrmSampleEvaluationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SrmSampleEvaluationDO::getUpdateTime)
                .orderByDesc(SrmSampleEvaluationDO::getId));
    }

    default SrmSampleEvaluationDO selectByEvaluationNo(String evaluationNo) {
        return selectOne(SrmSampleEvaluationDO::getEvaluationNo, evaluationNo);
    }

}
