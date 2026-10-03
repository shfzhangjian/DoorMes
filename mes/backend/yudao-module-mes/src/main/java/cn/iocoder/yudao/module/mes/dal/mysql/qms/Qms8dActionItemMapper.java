package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.Qms8dActionItemDO;
import java.util.Arrays;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface Qms8dActionItemMapper extends BaseMapperX<Qms8dActionItemDO> {

    default List<Qms8dActionItemDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapperX<Qms8dActionItemDO>()
                .eq(Qms8dActionItemDO::getReportId, reportId)
                .orderByAsc(Qms8dActionItemDO::getSort)
                .orderByAsc(Qms8dActionItemDO::getId));
    }

    default List<Qms8dActionItemDO> selectOpenListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapperX<Qms8dActionItemDO>()
                .eq(Qms8dActionItemDO::getReportId, reportId)
                .notIn(Qms8dActionItemDO::getItemStatus, Arrays.asList("DONE", "VERIFIED", "CANCELLED")));
    }

    default List<Qms8dActionItemDO> selectListByOwnerUserId(Long ownerUserId) {
        return selectList(new LambdaQueryWrapperX<Qms8dActionItemDO>()
                .eq(Qms8dActionItemDO::getOwnerUserId, ownerUserId));
    }
}
