package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierScopePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierScopeDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierScopeMapper extends BaseMapperX<SrmSupplierScopeDO> {

    default PageResult<SrmSupplierScopeDO> selectPage(SrmSupplierScopePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmSupplierScopeDO>()
                .likeIfPresent(SrmSupplierScopeDO::getScopeCode, reqVO.getScopeCode())
                .likeIfPresent(SrmSupplierScopeDO::getScopeName, reqVO.getScopeName())
                .eqIfPresent(SrmSupplierScopeDO::getStatus, reqVO.getStatus())
                .orderByAsc(SrmSupplierScopeDO::getSort)
                .orderByDesc(SrmSupplierScopeDO::getId));
    }

    default SrmSupplierScopeDO selectByCode(String scopeCode) {
        return selectOne(SrmSupplierScopeDO::getScopeCode, scopeCode);
    }

    default List<SrmSupplierScopeDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<SrmSupplierScopeDO>()
                .eq(SrmSupplierScopeDO::getStatus, "ENABLED")
                .orderByAsc(SrmSupplierScopeDO::getSort)
                .orderByDesc(SrmSupplierScopeDO::getId));
    }

    default List<SrmSupplierScopeDO> selectListByIds(Collection<Long> ids) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierScopeDO>()
                .in(SrmSupplierScopeDO::getId, ids)
                .orderByAsc(SrmSupplierScopeDO::getSort)
                .orderByDesc(SrmSupplierScopeDO::getId));
    }

}
