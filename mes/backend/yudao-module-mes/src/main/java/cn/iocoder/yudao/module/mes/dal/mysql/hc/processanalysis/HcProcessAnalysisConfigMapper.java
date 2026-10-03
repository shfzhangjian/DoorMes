package cn.iocoder.yudao.module.mes.dal.mysql.hc.processanalysis;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processanalysis.HcProcessAnalysisConfigDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcProcessAnalysisConfigMapper extends BaseMapperX<HcProcessAnalysisConfigDO> {

    default List<HcProcessAnalysisConfigDO> selectVisibleList(Long ownerUserId) {
        return selectList(new LambdaQueryWrapperX<HcProcessAnalysisConfigDO>()
                .and(wrapper -> wrapper
                        .eq(HcProcessAnalysisConfigDO::getOwnerUserId, ownerUserId)
                        .or()
                        .eq(HcProcessAnalysisConfigDO::getScopeType, "SHARED"))
                .orderByDesc(HcProcessAnalysisConfigDO::getDefaultFlag)
                .orderByDesc(HcProcessAnalysisConfigDO::getUpdateTime)
                .orderByDesc(HcProcessAnalysisConfigDO::getId));
    }

    default HcProcessAnalysisConfigDO selectByOwnerAndName(
            Long ownerUserId, String configName, Long excludeId) {
        LambdaQueryWrapperX<HcProcessAnalysisConfigDO> wrapper =
                new LambdaQueryWrapperX<HcProcessAnalysisConfigDO>()
                        .eq(HcProcessAnalysisConfigDO::getOwnerUserId, ownerUserId)
                        .eq(HcProcessAnalysisConfigDO::getConfigName, configName);
        if (excludeId != null) {
            wrapper.ne(HcProcessAnalysisConfigDO::getId, excludeId);
        }
        return selectOne(wrapper);
    }

    default void clearDefaultFlags(Long ownerUserId, Long excludeId) {
        LambdaUpdateWrapper<HcProcessAnalysisConfigDO> wrapper =
                new LambdaUpdateWrapper<HcProcessAnalysisConfigDO>()
                        .eq(HcProcessAnalysisConfigDO::getOwnerUserId, ownerUserId)
                        .eq(HcProcessAnalysisConfigDO::getDefaultFlag, true)
                        .set(HcProcessAnalysisConfigDO::getDefaultFlag, false);
        if (excludeId != null) {
            wrapper.ne(HcProcessAnalysisConfigDO::getId, excludeId);
        }
        update(null, wrapper);
    }

}
