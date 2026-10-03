package cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelMaterialDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProductModelMaterialMapper extends BaseMapperX<HcProductModelMaterialDO> {

    default List<HcProductModelMaterialDO> selectListByModelId(Long modelId) {
        return selectList(new LambdaQueryWrapperX<HcProductModelMaterialDO>()
                .eq(HcProductModelMaterialDO::getModelId, modelId)
                .orderByDesc(HcProductModelMaterialDO::getIsDefault)
                .orderByAsc(HcProductModelMaterialDO::getMaterialCode));
    }

    default HcProductModelMaterialDO selectFirstByModelCode(String modelCode) {
        return selectOne(new LambdaQueryWrapperX<HcProductModelMaterialDO>()
                .eq(HcProductModelMaterialDO::getModelCode, modelCode)
                .eq(HcProductModelMaterialDO::getDeleted, false)
                .orderByDesc(HcProductModelMaterialDO::getIsDefault)
                .orderByAsc(HcProductModelMaterialDO::getMaterialCode)
                .last("LIMIT 1"));
    }

    default List<HcProductModelMaterialDO> selectDefaultListByModelCode(String modelCode) {
        return selectList(new LambdaQueryWrapperX<HcProductModelMaterialDO>()
                .eq(HcProductModelMaterialDO::getModelCode, modelCode)
                .eq(HcProductModelMaterialDO::getIsDefault, true)
                .eq(HcProductModelMaterialDO::getDeleted, false)
                .orderByAsc(HcProductModelMaterialDO::getMaterialCode));
    }

    default List<HcProductModelMaterialDO> selectListByModelCode(String modelCode) {
        return selectList(new LambdaQueryWrapperX<HcProductModelMaterialDO>()
                .eq(HcProductModelMaterialDO::getModelCode, modelCode)
                .eq(HcProductModelMaterialDO::getDeleted, false)
                .orderByDesc(HcProductModelMaterialDO::getIsDefault)
                .orderByAsc(HcProductModelMaterialDO::getMaterialCode));
    }

    default void deleteByModelId(Long modelId) {
        delete(HcProductModelMaterialDO::getModelId, modelId);
    }

    default void deleteByModelIds(Collection<Long> modelIds) {
        deleteBatch(HcProductModelMaterialDO::getModelId, modelIds);
    }

}
