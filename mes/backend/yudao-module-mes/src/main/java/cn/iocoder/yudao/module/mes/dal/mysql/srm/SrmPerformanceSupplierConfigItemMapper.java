package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceSupplierConfigItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPerformanceSupplierConfigItemMapper extends BaseMapperX<SrmPerformanceSupplierConfigItemDO> {

    default List<SrmPerformanceSupplierConfigItemDO> selectListByConfigAndTemplateVersion(Long configId,
                                                                                          Long templateVersionId) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceSupplierConfigItemDO>()
                .eq(SrmPerformanceSupplierConfigItemDO::getConfigId, configId)
                .eq(SrmPerformanceSupplierConfigItemDO::getTemplateVersionId, templateVersionId)
                .orderByAsc(SrmPerformanceSupplierConfigItemDO::getGroupSort)
                .orderByAsc(SrmPerformanceSupplierConfigItemDO::getIndicatorSort)
                .orderByAsc(SrmPerformanceSupplierConfigItemDO::getId));
    }

    default SrmPerformanceSupplierConfigItemDO selectByConfigAndTemplateItem(Long configId, Long templateItemId) {
        return selectOne(new LambdaQueryWrapperX<SrmPerformanceSupplierConfigItemDO>()
                .eq(SrmPerformanceSupplierConfigItemDO::getConfigId, configId)
                .eq(SrmPerformanceSupplierConfigItemDO::getTemplateItemId, templateItemId));
    }

    default void deleteByConfigAndTemplateVersion(Long configId, Long templateVersionId) {
        delete(new LambdaQueryWrapperX<SrmPerformanceSupplierConfigItemDO>()
                .eq(SrmPerformanceSupplierConfigItemDO::getConfigId, configId)
                .eq(SrmPerformanceSupplierConfigItemDO::getTemplateVersionId, templateVersionId));
    }

}
