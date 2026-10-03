package cn.iocoder.yudao.module.mes.dal.mysql.supplier;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import cn.iocoder.yudao.module.mes.controller.admin.supplier.vo.*;

import java.util.Collection;
import java.util.List;

/**
 * 供应商主数据 Mapper
 *
 * @author 演示管理员
 */
@Mapper
public interface MesSupplierMapper extends BaseMapperX<MesSupplierDO> {

    List<MesSupplierDO> selectCandidateSelectPage(
            @Param("tenantId") Long tenantId,
            @Param("supplierCode") String supplierCode,
            @Param("supplierName") String supplierName,
            @Param("status") String status,
            @Param("scopeId") Long scopeId,
            @Param("distinctSupplier") boolean distinctSupplier,
            @Param("scopeRestricted") boolean scopeRestricted,
            @Param("accessibleScopeIds") Collection<Long> accessibleScopeIds,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize);

    Long selectCandidateSelectCount(
            @Param("tenantId") Long tenantId,
            @Param("supplierCode") String supplierCode,
            @Param("supplierName") String supplierName,
            @Param("status") String status,
            @Param("scopeId") Long scopeId,
            @Param("distinctSupplier") boolean distinctSupplier,
            @Param("scopeRestricted") boolean scopeRestricted,
            @Param("accessibleScopeIds") Collection<Long> accessibleScopeIds);

    default PageResult<MesSupplierDO> selectPage(MesSupplierPageReqVO reqVO) {
        LambdaQueryWrapperX<MesSupplierDO> wrapper = new LambdaQueryWrapperX<MesSupplierDO>()
                .eqIfPresent(MesSupplierDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(MesSupplierDO::getSupplierName, reqVO.getSupplierName())
                .likeIfPresent(MesSupplierDO::getUsingDepartment, reqVO.getUsingDepartment())
                .likeIfPresent(MesSupplierDO::getShortName, reqVO.getShortName())
                .eqIfPresent(MesSupplierDO::getContactPerson, reqVO.getContactPerson())
                .eqIfPresent(MesSupplierDO::getContactPhone, reqVO.getContactPhone())
                .eqIfPresent(MesSupplierDO::getEmail, reqVO.getEmail())
                .eqIfPresent(MesSupplierDO::getAddress, reqVO.getAddress())
                .likeIfPresent(MesSupplierDO::getCompanyNature, reqVO.getCompanyNature())
                .likeIfPresent(MesSupplierDO::getLegalPerson, reqVO.getLegalPerson())
                .eqIfPresent(MesSupplierDO::getRegisteredCapital, reqVO.getRegisteredCapital())
                .betweenIfPresent(MesSupplierDO::getEstablishDate, reqVO.getEstablishDate())
                .likeIfPresent(MesSupplierDO::getOriginPlace, reqVO.getOriginPlace())
                .likeIfPresent(MesSupplierDO::getOriginalFactoryInfo, reqVO.getOriginalFactoryInfo())
                .likeIfPresent(MesSupplierDO::getMainProducts, reqVO.getMainProducts())
                .likeIfPresent(MesSupplierDO::getProvidedProduct, reqVO.getProvidedProduct())
                .likeIfPresent(MesSupplierDO::getModel, reqVO.getModel())
                .likeIfPresent(MesSupplierDO::getMaterialCode, reqVO.getMaterialCode())
                .eqIfPresent(MesSupplierDO::getMaterialCode, reqVO.getMaterialCodeExact())
                .likeIfPresent(MesSupplierDO::getApplicableProduct, reqVO.getApplicableProduct())
                .betweenIfPresent(MesSupplierDO::getImportDate, reqVO.getImportDate())
                .likeIfPresent(MesSupplierDO::getMaterialCategory, reqVO.getMaterialCategory())
                .eqIfPresent(MesSupplierDO::getMaterialGrade, reqVO.getMaterialGrade())
                .likeIfPresent(MesSupplierDO::getPaymentTerms, reqVO.getPaymentTerms())
                .likeIfPresent(MesSupplierDO::getDeliveryMethod, reqVO.getDeliveryMethod())
                .eqIfPresent(MesSupplierDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesSupplierDO::getScopeId, reqVO.getScopeId())
                .likeIfPresent(MesSupplierDO::getScopeName, reqVO.getScopeName())
                .eqIfPresent(MesSupplierDO::getLevel, reqVO.getLevel())
                .eqIfPresent(MesSupplierDO::getRemark, reqVO.getRemark())
                .eqIfPresent(MesSupplierDO::getSort, reqVO.getSort())
                .eqIfPresent(MesSupplierDO::getVersion, reqVO.getVersion())
                .betweenIfPresent(MesSupplierDO::getCreateTime, reqVO.getCreateTime());
        if (Boolean.TRUE.equals(reqVO.getScopeRestricted())) {
            wrapper.in(MesSupplierDO::getScopeId, reqVO.getAccessibleScopeIds());
        }
        if (StrUtil.isNotBlank(reqVO.getSupplierInfo())) {
            wrapper.and(query -> query.like(MesSupplierDO::getSupplierName, reqVO.getSupplierInfo())
                    .or()
                    .like(MesSupplierDO::getSupplierCode, reqVO.getSupplierInfo()));
        }
        return selectPage(reqVO, wrapper.orderByDesc(MesSupplierDO::getId));
    }

}
