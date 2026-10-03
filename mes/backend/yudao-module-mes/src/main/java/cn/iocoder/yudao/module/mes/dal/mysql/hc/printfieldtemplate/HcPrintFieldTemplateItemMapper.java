package cn.iocoder.yudao.module.mes.dal.mysql.hc.printfieldtemplate;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.printfieldtemplate.HcPrintFieldTemplateItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcPrintFieldTemplateItemMapper extends BaseMapperX<HcPrintFieldTemplateItemDO> {

    default List<HcPrintFieldTemplateItemDO> selectListByTemplateId(Long templateId) {
        return selectList(new LambdaQueryWrapperX<HcPrintFieldTemplateItemDO>()
                .eq(HcPrintFieldTemplateItemDO::getTemplateId, templateId)
                .orderByAsc(HcPrintFieldTemplateItemDO::getSort)
                .orderByAsc(HcPrintFieldTemplateItemDO::getId));
    }

    default List<HcPrintFieldTemplateItemDO> selectVisibleListByTemplateId(Long templateId) {
        return selectList(new LambdaQueryWrapperX<HcPrintFieldTemplateItemDO>()
                .eq(HcPrintFieldTemplateItemDO::getTemplateId, templateId)
                .eq(HcPrintFieldTemplateItemDO::getVisible, true)
                .orderByAsc(HcPrintFieldTemplateItemDO::getSort)
                .orderByAsc(HcPrintFieldTemplateItemDO::getId));
    }

    @Delete("DELETE FROM mes_hc_print_field_template_item WHERE template_id = #{templateId}")
    int physicalDeleteByTemplateId(@Param("templateId") Long templateId);

}
