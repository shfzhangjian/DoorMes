package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierExitApprovalSignDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierExitApprovalSignMapper extends BaseMapperX<SrmSupplierExitApprovalSignDO> {

    default List<SrmSupplierExitApprovalSignDO> selectListByApplyId(Long applyId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierExitApprovalSignDO>()
                .eq(SrmSupplierExitApprovalSignDO::getApplyId, applyId)
                .orderByAsc(SrmSupplierExitApprovalSignDO::getDeptName)
                .orderByAsc(SrmSupplierExitApprovalSignDO::getUserName)
                .orderByAsc(SrmSupplierExitApprovalSignDO::getId));
    }

    default SrmSupplierExitApprovalSignDO selectByApplyIdAndUserId(Long applyId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<SrmSupplierExitApprovalSignDO>()
                .eq(SrmSupplierExitApprovalSignDO::getApplyId, applyId)
                .eq(SrmSupplierExitApprovalSignDO::getUserId, userId)
                .eq(SrmSupplierExitApprovalSignDO::getSignStatus, "PENDING")
                .last("LIMIT 1"));
    }

    default List<SrmSupplierExitApprovalSignDO> selectPendingListByApplyIdAndUserId(Long applyId, Long userId) {
        return selectList(new LambdaQueryWrapperX<SrmSupplierExitApprovalSignDO>()
                .eq(SrmSupplierExitApprovalSignDO::getApplyId, applyId)
                .eq(SrmSupplierExitApprovalSignDO::getUserId, userId)
                .eq(SrmSupplierExitApprovalSignDO::getSignStatus, "PENDING")
                .orderByAsc(SrmSupplierExitApprovalSignDO::getId));
    }

    default void deleteByApplyId(Long applyId) {
        delete(SrmSupplierExitApprovalSignDO::getApplyId, applyId);
    }

}
