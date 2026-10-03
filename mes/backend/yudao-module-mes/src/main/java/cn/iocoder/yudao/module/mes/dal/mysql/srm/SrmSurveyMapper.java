package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSurveyPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSurveyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSurveyMapper extends BaseMapperX<SrmSurveyDO> {

    default PageResult<SrmSurveyDO> selectPage(SrmSurveyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmSurveyDO>()
                .eqIfPresent(SrmSurveyDO::getSurveyNo, reqVO.getSurveyNo())
                .eqIfPresent(SrmSurveyDO::getSupplierId, reqVO.getSupplierId())
                .likeIfPresent(SrmSurveyDO::getSupplierName, reqVO.getSupplierName())
                .eqIfPresent(SrmSurveyDO::getUnregisteredSupplier, reqVO.getUnregisteredSupplier())
                .eqIfPresent(SrmSurveyDO::getSupplierSourceType, reqVO.getSupplierSourceType())
                .likeIfPresent(SrmSurveyDO::getSurveyLeaderName, reqVO.getSurveyLeaderName())
                .eqIfPresent(SrmSurveyDO::getNature, reqVO.getNature())
                .eqIfPresent(SrmSurveyDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SrmSurveyDO::getSurveyDate, reqVO.getSurveyDate())
                .betweenIfPresent(SrmSurveyDO::getApplyTime, reqVO.getApplyTime())
                .betweenIfPresent(SrmSurveyDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SrmSurveyDO::getSurveyDate)
                .orderByDesc(SrmSurveyDO::getApplyTime)
                .orderByDesc(SrmSurveyDO::getId));
    }

    default SrmSurveyDO selectBySurveyNo(String surveyNo) {
        return selectOne(SrmSurveyDO::getSurveyNo, surveyNo);
    }

}
