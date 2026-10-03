package cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelSegmentDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProductModelSegmentMapper extends BaseMapperX<HcProductModelSegmentDO> {

    default List<HcProductModelSegmentDO> selectListByModelId(Long modelId) {
        return selectList(new LambdaQueryWrapperX<HcProductModelSegmentDO>()
                .eq(HcProductModelSegmentDO::getModelId, modelId)
                .orderByAsc(HcProductModelSegmentDO::getSort)
                .orderByAsc(HcProductModelSegmentDO::getId));
    }

    default void deleteByModelId(Long modelId) {
        delete(HcProductModelSegmentDO::getModelId, modelId);
    }

    default void deleteByModelIds(Collection<Long> modelIds) {
        deleteBatch(HcProductModelSegmentDO::getModelId, modelIds);
    }

}
