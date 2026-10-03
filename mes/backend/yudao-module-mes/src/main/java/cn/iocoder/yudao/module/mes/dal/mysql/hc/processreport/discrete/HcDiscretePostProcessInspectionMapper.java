package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.discrete;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.discrete.HcDiscretePostProcessInspectionDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcDiscretePostProcessInspectionMapper extends BaseMapperX<HcDiscretePostProcessInspectionDO> {

    default HcDiscretePostProcessInspectionDO selectLatestByLockId(Long lockId) {
        if (lockId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcDiscretePostProcessInspectionDO>()
                .eq(HcDiscretePostProcessInspectionDO::getSourceLockId, lockId)
                .eq(HcDiscretePostProcessInspectionDO::getDeleted, false)
                .orderByDesc(HcDiscretePostProcessInspectionDO::getId)
                .last("LIMIT 1"));
    }

    default HcDiscretePostProcessInspectionDO selectLatestByLockIdAndType(Long lockId, String inspectionType) {
        if (lockId == null || inspectionType == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcDiscretePostProcessInspectionDO>()
                .eq(HcDiscretePostProcessInspectionDO::getSourceLockId, lockId)
                .eq(HcDiscretePostProcessInspectionDO::getInspectionType, inspectionType)
                .eq(HcDiscretePostProcessInspectionDO::getDeleted, false)
                .orderByDesc(HcDiscretePostProcessInspectionDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcDiscretePostProcessInspectionDO> selectListByLockIds(Collection<Long> lockIds) {
        if (lockIds == null || lockIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcDiscretePostProcessInspectionDO>()
                .in(HcDiscretePostProcessInspectionDO::getSourceLockId, lockIds)
                .eq(HcDiscretePostProcessInspectionDO::getDeleted, false)
                .orderByDesc(HcDiscretePostProcessInspectionDO::getId));
    }
}
