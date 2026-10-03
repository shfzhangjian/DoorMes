package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSurveyReviewDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSurveyReviewMapper extends BaseMapperX<SrmSurveyReviewDO> {

    default List<SrmSurveyReviewDO> selectBySurveyId(Long surveyId) {
        return selectList(new LambdaQueryWrapperX<SrmSurveyReviewDO>()
                .eq(SrmSurveyReviewDO::getSurveyId, surveyId)
                .orderByAsc(SrmSurveyReviewDO::getSort)
                .orderByAsc(SrmSurveyReviewDO::getId));
    }

    default int deleteBySurveyId(Long surveyId) {
        return delete(new LambdaQueryWrapperX<SrmSurveyReviewDO>()
                .eq(SrmSurveyReviewDO::getSurveyId, surveyId));
    }

}
