package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmAttachmentDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmAttachmentMapper extends BaseMapperX<SrmAttachmentDO> {

    default List<SrmAttachmentDO> selectByBiz(String bizType, Long bizId, boolean includeHistory) {
        LambdaQueryWrapperX<SrmAttachmentDO> query = new LambdaQueryWrapperX<SrmAttachmentDO>()
                .eq(SrmAttachmentDO::getBizType, bizType)
                .eq(SrmAttachmentDO::getBizId, bizId)
                .eqIfPresent(SrmAttachmentDO::getLatestVersion, includeHistory ? null : Boolean.TRUE)
                .orderByDesc(SrmAttachmentDO::getLatestVersion)
                .orderByDesc(SrmAttachmentDO::getVersionTime)
                .orderByDesc(SrmAttachmentDO::getVersionNo)
                .orderByDesc(SrmAttachmentDO::getId);
        return selectList(query);
    }

    default List<SrmAttachmentDO> selectByVersionGroup(String versionGroupNo) {
        return selectList(new LambdaQueryWrapperX<SrmAttachmentDO>()
                .eq(SrmAttachmentDO::getVersionGroupNo, versionGroupNo)
                .orderByDesc(SrmAttachmentDO::getVersionNo)
                .orderByDesc(SrmAttachmentDO::getId));
    }

    default int updateLatestVersion(Long id, boolean expectedLatest, boolean latestVersion) {
        return update(null, new LambdaUpdateWrapper<SrmAttachmentDO>()
                .set(SrmAttachmentDO::getLatestVersion, latestVersion)
                .eq(SrmAttachmentDO::getId, id)
                .eq(SrmAttachmentDO::getLatestVersion, expectedLatest));
    }

    default int deleteByBiz(String bizType, Long bizId) {
        return delete(new LambdaQueryWrapperX<SrmAttachmentDO>()
                .eq(SrmAttachmentDO::getBizType, bizType)
                .eq(SrmAttachmentDO::getBizId, bizId));
    }

    default int deleteByBizIds(String bizType, Collection<Long> bizIds) {
        if (bizIds == null || bizIds.isEmpty()) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<SrmAttachmentDO>()
                .eq(SrmAttachmentDO::getBizType, bizType)
                .in(SrmAttachmentDO::getBizId, bizIds));
    }

}
