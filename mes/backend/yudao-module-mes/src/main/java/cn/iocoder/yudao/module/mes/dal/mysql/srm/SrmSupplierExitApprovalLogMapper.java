package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierExitApprovalLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierExitApprovalLogMapper extends BaseMapperX<SrmSupplierExitApprovalLogDO> {

    default List<SrmSupplierExitApprovalLogDO> selectListByApplyId(Long applyId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierExitApprovalLogDO>()
                .eq(SrmSupplierExitApprovalLogDO::getApplyId, applyId)
                .orderByAsc(SrmSupplierExitApprovalLogDO::getCreateTime)
                .orderByAsc(SrmSupplierExitApprovalLogDO::getId));
    }

}
