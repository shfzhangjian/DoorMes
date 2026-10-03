package cn.iocoder.yudao.module.mes.dal.mysql.hc.printfieldtemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.printfieldtemplate.HcPrintFieldTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPrintFieldTemplateMapper extends BaseMapperX<HcPrintFieldTemplateDO> {

    default PageResult<HcPrintFieldTemplateDO> selectPage(HcPrintFieldTemplatePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default HcPrintFieldTemplateDO selectByTemplateCode(String templateCode) {
        return selectOne(new LambdaQueryWrapperX<HcPrintFieldTemplateDO>()
                .eq(HcPrintFieldTemplateDO::getTemplateCode, templateCode));
    }

    default HcPrintFieldTemplateDO selectActiveByTemplateCode(String templateCode) {
        return selectOne(new LambdaQueryWrapperX<HcPrintFieldTemplateDO>()
                .eq(HcPrintFieldTemplateDO::getTemplateCode, templateCode)
                .eq(HcPrintFieldTemplateDO::getStatus, 0)
                .last("LIMIT 1"));
    }

    default LambdaQueryWrapperX<HcPrintFieldTemplateDO> buildQuery(HcPrintFieldTemplatePageReqVO reqVO) {
        LambdaQueryWrapperX<HcPrintFieldTemplateDO> query = new LambdaQueryWrapperX<HcPrintFieldTemplateDO>()
                .likeIfPresent(HcPrintFieldTemplateDO::getTemplateCode, reqVO.getTemplateCode())
                .likeIfPresent(HcPrintFieldTemplateDO::getTemplateName, reqVO.getTemplateName())
                .eqIfPresent(HcPrintFieldTemplateDO::getProcessCode, reqVO.getProcessCode())
                .eqIfPresent(HcPrintFieldTemplateDO::getDocumentType, reqVO.getDocumentType())
                .eqIfPresent(HcPrintFieldTemplateDO::getStatus, reqVO.getStatus());
        query.orderByAsc(HcPrintFieldTemplateDO::getProcessCode);
        query.orderByAsc(HcPrintFieldTemplateDO::getDocumentType);
        query.orderByAsc(HcPrintFieldTemplateDO::getId);
        return query;
    }

}
