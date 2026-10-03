package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiRecheckApplyDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QmsFaiRecheckApplyMapper extends BaseMapperX<QmsFaiRecheckApplyDO> {

    String STATUS_PENDING_AUDIT = "PENDING_AUDIT";

    @Select("SELECT * FROM mes_qms_fai_recheck_apply WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    QmsFaiRecheckApplyDO selectByIdForUpdate(@Param("id") Long id);

    default QmsFaiRecheckApplyDO selectPendingBySourceFaiId(Long sourceFaiId) {
        return selectOne(new LambdaQueryWrapperX<QmsFaiRecheckApplyDO>()
                .eq(QmsFaiRecheckApplyDO::getSourceFaiId, sourceFaiId)
                .eq(QmsFaiRecheckApplyDO::getStatus, STATUS_PENDING_AUDIT)
                .orderByDesc(QmsFaiRecheckApplyDO::getId)
                .last("LIMIT 1"));
    }

    default QmsFaiRecheckApplyDO selectLatestBySourceFaiId(Long sourceFaiId) {
        return selectOne(new LambdaQueryWrapperX<QmsFaiRecheckApplyDO>()
                .eq(QmsFaiRecheckApplyDO::getSourceFaiId, sourceFaiId)
                .orderByDesc(QmsFaiRecheckApplyDO::getId)
                .last("LIMIT 1"));
    }

    default List<QmsFaiRecheckApplyDO> selectLatestListBySourceFaiIds(Collection<Long> sourceFaiIds) {
        if (sourceFaiIds == null || sourceFaiIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsFaiRecheckApplyDO>()
                .in(QmsFaiRecheckApplyDO::getSourceFaiId, sourceFaiIds)
                .orderByDesc(QmsFaiRecheckApplyDO::getId));
    }
}
