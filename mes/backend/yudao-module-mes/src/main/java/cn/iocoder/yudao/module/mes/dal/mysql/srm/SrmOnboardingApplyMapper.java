package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplyPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmOnboardingApplyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmOnboardingApplyMapper extends BaseMapperX<SrmOnboardingApplyDO> {

    default PageResult<SrmOnboardingApplyDO> selectPage(SrmOnboardingApplyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmOnboardingApplyDO>()
                .eqIfPresent(SrmOnboardingApplyDO::getApplyNo, reqVO.getApplyNo())
                .eqIfPresent(SrmOnboardingApplyDO::getSupplierId, reqVO.getSupplierId())
                .likeIfPresent(SrmOnboardingApplyDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(SrmOnboardingApplyDO::getSupplierName, reqVO.getSupplierName())
                .likeIfPresent(SrmOnboardingApplyDO::getMaterialName, reqVO.getMaterialName())
                .likeIfPresent(SrmOnboardingApplyDO::getMaterialModel, reqVO.getMaterialModel())
                .likeIfPresent(SrmOnboardingApplyDO::getApplicableProduct, reqVO.getApplicableProduct())
                .eqIfPresent(SrmOnboardingApplyDO::getImportType, reqVO.getImportType())
                .likeIfPresent(SrmOnboardingApplyDO::getApplyReason, reqVO.getApplyReason())
                .eqIfPresent(SrmOnboardingApplyDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SrmOnboardingApplyDO::getApplyTime, reqVO.getApplyTime())
                .betweenIfPresent(SrmOnboardingApplyDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SrmOnboardingApplyDO::getApplyTime)
                .orderByDesc(SrmOnboardingApplyDO::getId));
    }

    default SrmOnboardingApplyDO selectByApplyNo(String applyNo) {
        return selectOne(SrmOnboardingApplyDO::getApplyNo, applyNo);
    }

}
