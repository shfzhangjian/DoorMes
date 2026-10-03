package cn.iocoder.yudao.module.mes.dal.mysql.hc.ocaptemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo.HcOcapTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.ocaptemplate.HcOcapTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcOcapTemplateMapper extends BaseMapperX<HcOcapTemplateDO> {

    default PageResult<HcOcapTemplateDO> selectPage(HcOcapTemplatePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcOcapTemplateDO> selectList(HcOcapTemplatePageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcOcapTemplateDO> buildQuery(HcOcapTemplatePageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcOcapTemplateDO>()
                .likeIfPresent(HcOcapTemplateDO::getOcapCode, reqVO.getOcapCode())
                .likeIfPresent(HcOcapTemplateDO::getOcapName, reqVO.getOcapName())
                .eqIfPresent(HcOcapTemplateDO::getBusinessStage, reqVO.getBusinessStage())
                .likeIfPresent(HcOcapTemplateDO::getTriggerItemCode, reqVO.getTriggerItemCode())
                .eqIfPresent(HcOcapTemplateDO::getTriggerCondition, reqVO.getTriggerCondition())
                .eqIfPresent(HcOcapTemplateDO::getActionSteps, reqVO.getActionSteps())
                .eqIfPresent(HcOcapTemplateDO::getStatus, reqVO.getStatus())
                .orderByDesc(HcOcapTemplateDO::getId);
    }
}