package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryEvaluationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPreliminaryEvaluationMapper extends BaseMapperX<SrmPreliminaryEvaluationDO> {

    default PageResult<SrmPreliminaryEvaluationDO> selectPage(SrmPreliminaryEvaluationPageReqVO reqVO) {
        LambdaQueryWrapperX<SrmPreliminaryEvaluationDO> query = new LambdaQueryWrapperX<SrmPreliminaryEvaluationDO>()
                .likeIfPresent(SrmPreliminaryEvaluationDO::getEvaluationNo, reqVO.getEvaluationNo())
                .likeIfPresent(SrmPreliminaryEvaluationDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(SrmPreliminaryEvaluationDO::getSupplierName, reqVO.getSupplierName())
                .eqIfPresent(SrmPreliminaryEvaluationDO::getProjectId, reqVO.getProjectId())
                .likeIfPresent(SrmPreliminaryEvaluationDO::getProjectCode, reqVO.getProjectCode())
                .likeIfPresent(SrmPreliminaryEvaluationDO::getProjectName, reqVO.getProjectName())
                .eqIfPresent(SrmPreliminaryEvaluationDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SrmPreliminaryEvaluationDO::getTemplateId, reqVO.getTemplateId())
                .betweenIfPresent(SrmPreliminaryEvaluationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SrmPreliminaryEvaluationDO::getUpdateTime)
                .orderByDesc(SrmPreliminaryEvaluationDO::getId);
        if (reqVO.getVisibleEvaluationIds() != null) {
            if (CollUtil.isEmpty(reqVO.getVisibleEvaluationIds())) {
                query.eq(SrmPreliminaryEvaluationDO::getId, -1L);
            } else {
                query.in(SrmPreliminaryEvaluationDO::getId, reqVO.getVisibleEvaluationIds());
            }
        }
        return selectPage(reqVO, query);
    }

    default SrmPreliminaryEvaluationDO selectByEvaluationNo(String evaluationNo) {
        return selectOne(SrmPreliminaryEvaluationDO::getEvaluationNo, evaluationNo);
    }

}
