package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmEvaluationTemplateMapper extends BaseMapperX<SrmEvaluationTemplateDO> {

    default PageResult<SrmEvaluationTemplateDO> selectPage(SrmEvaluationTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmEvaluationTemplateDO>()
                .likeIfPresent(SrmEvaluationTemplateDO::getTemplateCode, reqVO.getTemplateCode())
                .likeIfPresent(SrmEvaluationTemplateDO::getTemplateName, reqVO.getTemplateName())
                .eqIfPresent(SrmEvaluationTemplateDO::getSceneType, reqVO.getSceneType())
                .eqIfPresent(SrmEvaluationTemplateDO::getStatus, reqVO.getStatus())
                .orderByDesc(SrmEvaluationTemplateDO::getUpdateTime)
                .orderByDesc(SrmEvaluationTemplateDO::getId));
    }

    default SrmEvaluationTemplateDO selectByCode(String templateCode) {
        return selectOne(SrmEvaluationTemplateDO::getTemplateCode, templateCode);
    }

}
