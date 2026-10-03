package cn.iocoder.yudao.module.mes.dal.mysql.hc.customerprinttemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.customerprinttemplate.HcCustomerPrintTemplateDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcCustomerPrintTemplateMapper extends BaseMapperX<HcCustomerPrintTemplateDO> {

    default PageResult<HcCustomerPrintTemplateDO> selectPage(HcCustomerPrintTemplatePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default HcCustomerPrintTemplateDO selectByTemplateCode(String templateCode) {
        return selectOne(new LambdaQueryWrapperX<HcCustomerPrintTemplateDO>()
                .eq(HcCustomerPrintTemplateDO::getTemplateCode, templateCode));
    }

    default List<HcCustomerPrintTemplateDO> selectActiveList(String templateType, String customerCode, String customerName) {
        return selectList(new LambdaQueryWrapperX<HcCustomerPrintTemplateDO>()
                .eq(HcCustomerPrintTemplateDO::getStatus, 0)
                .eqIfPresent(HcCustomerPrintTemplateDO::getTemplateType, templateType)
                .eqIfPresent(HcCustomerPrintTemplateDO::getCustomerCode, customerCode)
                .likeIfPresent(HcCustomerPrintTemplateDO::getCustomerName, customerName)
                .orderByAsc(HcCustomerPrintTemplateDO::getCustomerCode)
                .orderByAsc(HcCustomerPrintTemplateDO::getTemplateName)
                .orderByAsc(HcCustomerPrintTemplateDO::getId));
    }

    default LambdaQueryWrapperX<HcCustomerPrintTemplateDO> buildQuery(HcCustomerPrintTemplatePageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcCustomerPrintTemplateDO>()
                .likeIfPresent(HcCustomerPrintTemplateDO::getTemplateCode, reqVO.getTemplateCode())
                .likeIfPresent(HcCustomerPrintTemplateDO::getTemplateName, reqVO.getTemplateName())
                .likeIfPresent(HcCustomerPrintTemplateDO::getCustomerCode, reqVO.getCustomerCode())
                .likeIfPresent(HcCustomerPrintTemplateDO::getCustomerName, reqVO.getCustomerName())
                .eqIfPresent(HcCustomerPrintTemplateDO::getTemplateType, reqVO.getTemplateType())
                .eqIfPresent(HcCustomerPrintTemplateDO::getStatus, reqVO.getStatus())
                .orderByDesc(HcCustomerPrintTemplateDO::getId);
    }

}
