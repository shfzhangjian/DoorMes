package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSampleDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsFaiSampleMapper extends BaseMapperX<QmsFaiSampleDO> {

    default List<QmsFaiSampleDO> selectListByFaiId(Long faiId) {
        return selectList(new LambdaQueryWrapper<QmsFaiSampleDO>()
                .eq(QmsFaiSampleDO::getFaiId, faiId)
                .orderByAsc(QmsFaiSampleDO::getFaiItemId)
                .orderByAsc(QmsFaiSampleDO::getSampleRole)
                .orderByAsc(QmsFaiSampleDO::getSampleSeq));
    }

    default List<QmsFaiSampleDO> selectListByFaiIdAndItemIdsAndRole(Long faiId, Collection<Long> faiItemIds,
                                                                    String sampleRole) {
        return selectList(new LambdaQueryWrapper<QmsFaiSampleDO>()
                .eq(QmsFaiSampleDO::getFaiId, faiId)
                .in(QmsFaiSampleDO::getFaiItemId, faiItemIds)
                .eq(QmsFaiSampleDO::getSampleRole, sampleRole)
                .orderByAsc(QmsFaiSampleDO::getFaiItemId)
                .orderByAsc(QmsFaiSampleDO::getSampleSeq));
    }

    default List<QmsFaiSampleDO> selectListByFaiIdAndItemIds(Long faiId, Collection<Long> faiItemIds) {
        return selectList(new LambdaQueryWrapper<QmsFaiSampleDO>()
                .eq(QmsFaiSampleDO::getFaiId, faiId)
                .in(QmsFaiSampleDO::getFaiItemId, faiItemIds)
                .orderByAsc(QmsFaiSampleDO::getFaiItemId)
                .orderByAsc(QmsFaiSampleDO::getSampleRole)
                .orderByAsc(QmsFaiSampleDO::getSampleSeq));
    }

    default int deleteByFaiId(Long faiId) {
        return delete(new LambdaQueryWrapper<QmsFaiSampleDO>()
                .eq(QmsFaiSampleDO::getFaiId, faiId));
    }

    default int deleteByFaiIdAndRoles(Long faiId, Collection<String> sampleRoles) {
        return delete(new LambdaQueryWrapper<QmsFaiSampleDO>()
                .eq(QmsFaiSampleDO::getFaiId, faiId)
                .in(QmsFaiSampleDO::getSampleRole, sampleRoles));
    }

    default int deleteByFaiIdAndItemIdsAndRoles(Long faiId, Collection<Long> faiItemIds,
                                                Collection<String> sampleRoles) {
        return delete(new LambdaQueryWrapper<QmsFaiSampleDO>()
                .eq(QmsFaiSampleDO::getFaiId, faiId)
                .in(QmsFaiSampleDO::getFaiItemId, faiItemIds)
                .in(QmsFaiSampleDO::getSampleRole, sampleRoles));
    }

    default int deleteByFaiIds(Collection<Long> faiIds) {
        return delete(new LambdaQueryWrapper<QmsFaiSampleDO>()
                .in(QmsFaiSampleDO::getFaiId, faiIds));
    }
}
