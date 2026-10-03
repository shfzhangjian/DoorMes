package cn.iocoder.yudao.module.mes.dal.mysql.hc.customerprinttemplate;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.customerprinttemplate.HcCustomerPrintTemplateVarDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcCustomerPrintTemplateVarMapper extends BaseMapperX<HcCustomerPrintTemplateVarDO> {

    default List<HcCustomerPrintTemplateVarDO> selectListByTemplateId(Long templateId) {
        return selectList(new LambdaQueryWrapperX<HcCustomerPrintTemplateVarDO>()
                .eq(HcCustomerPrintTemplateVarDO::getTemplateId, templateId)
                .orderByAsc(HcCustomerPrintTemplateVarDO::getSort)
                .orderByAsc(HcCustomerPrintTemplateVarDO::getId));
    }

    @Delete("DELETE FROM mes_hc_customer_print_template_var WHERE template_id = #{templateId}")
    int physicalDeleteByTemplateId(@Param("templateId") Long templateId);

}
