package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierExitApprovalDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSupplierExitApprovalMapper extends BaseMapperX<SrmSupplierExitApprovalDO> {

    default PageResult<SrmSupplierExitApprovalDO> selectPage(SrmSupplierExitApprovalPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmSupplierExitApprovalDO>()
                .eqIfPresent(SrmSupplierExitApprovalDO::getExitNo, reqVO.getExitNo())
                .eqIfPresent(SrmSupplierExitApprovalDO::getSupplierId, reqVO.getSupplierId())
                .likeIfPresent(SrmSupplierExitApprovalDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(SrmSupplierExitApprovalDO::getSupplierName, reqVO.getSupplierName())
                .likeIfPresent(SrmSupplierExitApprovalDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(SrmSupplierExitApprovalDO::getMaterialModel, reqVO.getMaterialModel())
                .likeIfPresent(SrmSupplierExitApprovalDO::getExitReasonDesc, reqVO.getExitReasonDesc())
                .eqIfPresent(SrmSupplierExitApprovalDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SrmSupplierExitApprovalDO::getApplyTime, reqVO.getApplyTime())
                .betweenIfPresent(SrmSupplierExitApprovalDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SrmSupplierExitApprovalDO::getApplyTime)
                .orderByDesc(SrmSupplierExitApprovalDO::getId));
    }

    default SrmSupplierExitApprovalDO selectByExitNo(String exitNo) {
        return selectOne(SrmSupplierExitApprovalDO::getExitNo, exitNo);
    }

}
