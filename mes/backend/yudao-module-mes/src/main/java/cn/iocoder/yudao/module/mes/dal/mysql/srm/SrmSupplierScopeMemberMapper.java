package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierScopeMemberDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierScopeMemberMapper extends BaseMapperX<SrmSupplierScopeMemberDO> {

    default List<SrmSupplierScopeMemberDO> selectListByScopeId(Long scopeId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierScopeMemberDO>()
                .eq(SrmSupplierScopeMemberDO::getScopeId, scopeId)
                .orderByDesc(SrmSupplierScopeMemberDO::getId));
    }

    default List<SrmSupplierScopeMemberDO> selectListByScopeIds(Collection<Long> scopeIds) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierScopeMemberDO>()
                .in(SrmSupplierScopeMemberDO::getScopeId, scopeIds)
                .orderByDesc(SrmSupplierScopeMemberDO::getId));
    }

    default List<SrmSupplierScopeMemberDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierScopeMemberDO>()
                .eq(SrmSupplierScopeMemberDO::getUserId, userId)
                .orderByDesc(SrmSupplierScopeMemberDO::getId));
    }

    default SrmSupplierScopeMemberDO selectByScopeIdAndUserId(Long scopeId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<SrmSupplierScopeMemberDO>()
                .eq(SrmSupplierScopeMemberDO::getScopeId, scopeId)
                .eq(SrmSupplierScopeMemberDO::getUserId, userId)
                .last("LIMIT 1"));
    }

    default void deleteByScopeId(Long scopeId) {
        delete(new LambdaQueryWrapperX<SrmSupplierScopeMemberDO>()
                .eq(SrmSupplierScopeMemberDO::getScopeId, scopeId));
    }

}
