package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceSupplierConfigDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPerformanceSupplierConfigMapper extends BaseMapperX<SrmPerformanceSupplierConfigDO> {

    default PageResult<SrmPerformanceSupplierConfigDO> selectPage(SrmPerformanceSupplierConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmPerformanceSupplierConfigDO>()
                .likeIfPresent(SrmPerformanceSupplierConfigDO::getConfigNo, reqVO.getConfigNo())
                .eqIfPresent(SrmPerformanceSupplierConfigDO::getSupplierId, reqVO.getSupplierId())
                .likeIfPresent(SrmPerformanceSupplierConfigDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(SrmPerformanceSupplierConfigDO::getSupplierName, reqVO.getSupplierName())
                .eqIfPresent(SrmPerformanceSupplierConfigDO::getCurrentTemplateId, reqVO.getTemplateId())
                .eqIfPresent(SrmPerformanceSupplierConfigDO::getCurrentTemplateVersionId, reqVO.getTemplateVersionId())
                .eqIfPresent(SrmPerformanceSupplierConfigDO::getStatus, reqVO.getStatus())
                .orderByDesc(SrmPerformanceSupplierConfigDO::getUpdateTime)
                .orderByDesc(SrmPerformanceSupplierConfigDO::getId));
    }

    default SrmPerformanceSupplierConfigDO selectByConfigNo(String configNo) {
        return selectOne(SrmPerformanceSupplierConfigDO::getConfigNo, configNo);
    }

    default SrmPerformanceSupplierConfigDO selectBySupplierAndTemplateVersion(Long supplierId, Long templateVersionId) {
        return selectOne(new LambdaQueryWrapperX<SrmPerformanceSupplierConfigDO>()
                .eq(SrmPerformanceSupplierConfigDO::getSupplierId, supplierId)
                .eq(SrmPerformanceSupplierConfigDO::getCurrentTemplateVersionId, templateVersionId));
    }

    default List<SrmPerformanceSupplierConfigDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceSupplierConfigDO>()
                .eq(SrmPerformanceSupplierConfigDO::getStatus, "ENABLED")
                .orderByAsc(SrmPerformanceSupplierConfigDO::getSupplierName)
                .orderByDesc(SrmPerformanceSupplierConfigDO::getId));
    }

    default SrmPerformanceSupplierConfigDO selectEnabledBySupplierId(Long supplierId) {
        return selectOne(new LambdaQueryWrapperX<SrmPerformanceSupplierConfigDO>()
                .eq(SrmPerformanceSupplierConfigDO::getSupplierId, supplierId)
                .eq(SrmPerformanceSupplierConfigDO::getStatus, "ENABLED")
                .orderByDesc(SrmPerformanceSupplierConfigDO::getUpdateTime)
                .orderByDesc(SrmPerformanceSupplierConfigDO::getId)
                .last("LIMIT 1"));
    }

}
