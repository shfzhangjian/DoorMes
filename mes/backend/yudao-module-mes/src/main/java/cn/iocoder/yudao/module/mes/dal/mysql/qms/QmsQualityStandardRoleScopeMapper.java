package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRoleScopePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardRoleScopeDO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsQualityStandardRoleScopeMapper extends BaseMapperX<QmsQualityStandardRoleScopeDO> {

    default PageResult<QmsQualityStandardRoleScopeDO> selectPage(QmsQualityStandardRoleScopePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsQualityStandardRoleScopeDO>()
                .eq(QmsQualityStandardRoleScopeDO::getRoleId, reqVO.getRoleId())
                .eq(QmsQualityStandardRoleScopeDO::getScopeType, reqVO.getScopeType())
                .orderByDesc(QmsQualityStandardRoleScopeDO::getId));
    }

    default List<QmsQualityStandardRoleScopeDO> selectListByRoleId(Long roleId) {
        return selectList(new LambdaQueryWrapperX<QmsQualityStandardRoleScopeDO>()
                .eq(QmsQualityStandardRoleScopeDO::getRoleId, roleId));
    }

    default List<QmsQualityStandardRoleScopeDO> selectListByRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsQualityStandardRoleScopeDO>()
                .in(QmsQualityStandardRoleScopeDO::getRoleId, roleIds));
    }

    default List<QmsQualityStandardRoleScopeDO> selectListByRoleIdAndScopeType(Long roleId, String scopeType) {
        return selectList(new LambdaQueryWrapperX<QmsQualityStandardRoleScopeDO>()
                .eq(QmsQualityStandardRoleScopeDO::getRoleId, roleId)
                .eq(QmsQualityStandardRoleScopeDO::getScopeType, scopeType));
    }

    default List<QmsQualityStandardRoleScopeDO> selectListByRoleIdAndStandardIds(Long roleId,
                                                                                 Collection<Long> standardIds) {
        if (standardIds == null || standardIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<QmsQualityStandardRoleScopeDO>()
                .eq(QmsQualityStandardRoleScopeDO::getRoleId, roleId)
                .in(QmsQualityStandardRoleScopeDO::getStandardId, standardIds));
    }
}
