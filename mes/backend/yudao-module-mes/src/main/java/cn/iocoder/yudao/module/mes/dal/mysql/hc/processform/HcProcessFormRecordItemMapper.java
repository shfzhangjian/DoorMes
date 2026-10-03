package cn.iocoder.yudao.module.mes.dal.mysql.hc.processform;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProcessFormRecordItemMapper extends BaseMapperX<HcProcessFormRecordItemDO> {

    default List<HcProcessFormRecordItemDO> selectListByRecordId(Long recordId) {
        return selectList(new LambdaQueryWrapperX<HcProcessFormRecordItemDO>()
                .eq(HcProcessFormRecordItemDO::getRecordId, recordId)
                .orderByAsc(HcProcessFormRecordItemDO::getItemSeq)
                .orderByAsc(HcProcessFormRecordItemDO::getId));
    }

    default void deleteByRecordId(Long recordId) {
        delete(new LambdaQueryWrapperX<HcProcessFormRecordItemDO>()
                .eq(HcProcessFormRecordItemDO::getRecordId, recordId));
    }
}
