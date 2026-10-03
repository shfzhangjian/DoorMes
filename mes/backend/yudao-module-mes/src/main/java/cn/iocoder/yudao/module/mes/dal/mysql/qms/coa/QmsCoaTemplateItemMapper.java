package cn.iocoder.yudao.module.mes.dal.mysql.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaTemplateItemDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsCoaTemplateItemMapper extends BaseMapperX<QmsCoaTemplateItemDO> {
    default List<QmsCoaTemplateItemDO> selectListByTemplateId(Long templateId) {
        return selectList(new LambdaQueryWrapper<QmsCoaTemplateItemDO>()
                .eq(QmsCoaTemplateItemDO::getTemplateId, templateId)
                .orderByAsc(QmsCoaTemplateItemDO::getSortNo)
                .orderByAsc(QmsCoaTemplateItemDO::getId));
    }

    @Delete("DELETE FROM mes_qms_coa_template_item WHERE template_id = #{templateId}")
    int deleteByTemplateId(Long templateId);
}
