package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationProjectDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSampleEvaluationProjectMapper extends BaseMapperX<SrmSampleEvaluationProjectDO> {

    default PageResult<SrmSampleEvaluationProjectDO> selectPage(SrmSampleEvaluationProjectPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmSampleEvaluationProjectDO>()
                .likeIfPresent(SrmSampleEvaluationProjectDO::getProjectCode, reqVO.getProjectCode())
                .likeIfPresent(SrmSampleEvaluationProjectDO::getProjectName, reqVO.getProjectName())
                .eqIfPresent(SrmSampleEvaluationProjectDO::getStatus, reqVO.getStatus())
                .orderByDesc(SrmSampleEvaluationProjectDO::getUpdateTime)
                .orderByDesc(SrmSampleEvaluationProjectDO::getId));
    }

    default SrmSampleEvaluationProjectDO selectByProjectCode(String projectCode) {
        return selectOne(SrmSampleEvaluationProjectDO::getProjectCode, projectCode);
    }

    default List<SrmSampleEvaluationProjectDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<SrmSampleEvaluationProjectDO>()
                .eq(SrmSampleEvaluationProjectDO::getStatus, "ENABLED")
                .orderByAsc(SrmSampleEvaluationProjectDO::getProjectCode)
                .orderByDesc(SrmSampleEvaluationProjectDO::getId));
    }

}
