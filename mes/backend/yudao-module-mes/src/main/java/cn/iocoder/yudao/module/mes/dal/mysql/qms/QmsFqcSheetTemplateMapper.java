package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSheetTemplateDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFqcSheetTemplateMapper extends BaseMapperX<QmsFqcSheetTemplateDO> {

    default List<QmsFqcSheetTemplateDO> selectEnableList(String productModel) {
        return selectList(new LambdaQueryWrapperX<QmsFqcSheetTemplateDO>()
                .eq(QmsFqcSheetTemplateDO::getStatus, "ENABLE")
                .likeIfPresent(QmsFqcSheetTemplateDO::getProductModel, productModel)
                .orderByAsc(QmsFqcSheetTemplateDO::getTemplateCode)
                .orderByDesc(QmsFqcSheetTemplateDO::getTemplateVersion));
    }
}
