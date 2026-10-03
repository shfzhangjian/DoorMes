package cn.iocoder.yudao.module.mes.dal.mysql.hc.formtemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo.HcFormTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate.HcFormTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcFormTemplateMapper extends BaseMapperX<HcFormTemplateDO> {

    default PageResult<HcFormTemplateDO> selectPage(HcFormTemplatePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcFormTemplateDO> selectList(HcFormTemplatePageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcFormTemplateDO> buildQuery(HcFormTemplatePageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcFormTemplateDO>()
                .likeIfPresent(HcFormTemplateDO::getTemplateCode, reqVO.getTemplateCode())
                .likeIfPresent(HcFormTemplateDO::getTemplateName, reqVO.getTemplateName())
                .eqIfPresent(HcFormTemplateDO::getTemplateType, reqVO.getTemplateType())
                .eqIfPresent(HcFormTemplateDO::getBusinessStage, reqVO.getBusinessStage())
                .eqIfPresent(HcFormTemplateDO::getFormStyle, reqVO.getFormStyle())
                .eqIfPresent(HcFormTemplateDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HcFormTemplateDO::getRemark, reqVO.getRemark())
                .orderByDesc(HcFormTemplateDO::getId);
    }
}