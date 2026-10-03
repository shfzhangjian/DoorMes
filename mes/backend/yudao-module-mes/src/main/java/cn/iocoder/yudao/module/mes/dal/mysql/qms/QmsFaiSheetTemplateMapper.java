package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetTemplateDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFaiSheetTemplateMapper extends BaseMapperX<QmsFaiSheetTemplateDO> {

    default List<QmsFaiSheetTemplateDO> selectEnableList(String productModel) {
        return selectList(new LambdaQueryWrapperX<QmsFaiSheetTemplateDO>()
                .eq(QmsFaiSheetTemplateDO::getStatus, "ENABLE")
                .likeIfPresent(QmsFaiSheetTemplateDO::getProductModel, productModel)
                .orderByAsc(QmsFaiSheetTemplateDO::getTemplateCode)
                .orderByDesc(QmsFaiSheetTemplateDO::getTemplateVersion));
    }
}
