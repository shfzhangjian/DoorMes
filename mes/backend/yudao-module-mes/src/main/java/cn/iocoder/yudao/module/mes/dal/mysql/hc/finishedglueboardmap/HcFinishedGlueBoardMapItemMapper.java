package cn.iocoder.yudao.module.mes.dal.mysql.hc.finishedglueboardmap;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapItemDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcFinishedGlueBoardMapItemMapper extends BaseMapperX<HcFinishedGlueBoardMapItemDO> {

    default List<HcFinishedGlueBoardMapItemDO> selectListByMapId(Long mapId) {
        return selectList(new LambdaQueryWrapperX<HcFinishedGlueBoardMapItemDO>()
                .eq(HcFinishedGlueBoardMapItemDO::getMapId, mapId)
                .orderByAsc(HcFinishedGlueBoardMapItemDO::getGlueProcess)
                .orderByDesc(HcFinishedGlueBoardMapItemDO::getPreferredFlag)
                .orderByAsc(HcFinishedGlueBoardMapItemDO::getSort)
                .orderByAsc(HcFinishedGlueBoardMapItemDO::getId));
    }

    default void deleteByMapId(Long mapId) {
        delete(HcFinishedGlueBoardMapItemDO::getMapId, mapId);
    }

    default void deleteByMapIds(Collection<Long> mapIds) {
        deleteBatch(HcFinishedGlueBoardMapItemDO::getMapId, mapIds);
    }

}
