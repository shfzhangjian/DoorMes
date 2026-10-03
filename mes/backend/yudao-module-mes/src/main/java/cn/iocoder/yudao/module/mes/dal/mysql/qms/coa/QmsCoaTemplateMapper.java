package cn.iocoder.yudao.module.mes.dal.mysql.qms.coa;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.TemplatePageReq;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaTemplateDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

@Mapper
public interface QmsCoaTemplateMapper extends BaseMapperX<QmsCoaTemplateDO> {

    default PageResult<QmsCoaTemplateDO> selectPage(TemplatePageReq reqVO) {
        LambdaQueryWrapperX<QmsCoaTemplateDO> wrapper = new LambdaQueryWrapperX<QmsCoaTemplateDO>()
                .likeIfPresent(QmsCoaTemplateDO::getCustomerName, reqVO.getCustomerName())
                .likeIfPresent(QmsCoaTemplateDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(QmsCoaTemplateDO::getProductModelCode, reqVO.getProductModelCode())
                .eqIfPresent(QmsCoaTemplateDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(QmsCoaTemplateDO::getStatus, reqVO.getStatus());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            wrapper.and(q -> q.like(QmsCoaTemplateDO::getTemplateCode, reqVO.getKeyword())
                    .or().like(QmsCoaTemplateDO::getTemplateName, reqVO.getKeyword())
                    .or().like(QmsCoaTemplateDO::getCustomerProductCode, reqVO.getKeyword())
                    .or().like(QmsCoaTemplateDO::getCustomerProductName, reqVO.getKeyword())
                    .or().like(QmsCoaTemplateDO::getCustomerCode, reqVO.getKeyword())
                    .or().like(QmsCoaTemplateDO::getCustomerName, reqVO.getKeyword())
                    .or().like(QmsCoaTemplateDO::getProductModelCode, reqVO.getKeyword())
                    .or().like(QmsCoaTemplateDO::getProductModelName, reqVO.getKeyword()));
        }
        return selectPage(reqVO, wrapper.orderByDesc(QmsCoaTemplateDO::getId));
    }

    default QmsCoaTemplateDO selectByCodeAndVersion(String code, String version, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<QmsCoaTemplateDO>()
                .eq(QmsCoaTemplateDO::getTemplateCode, code)
                .eq(QmsCoaTemplateDO::getVersionNo, version)
                .neIfPresent(QmsCoaTemplateDO::getId, excludeId));
    }

    default List<QmsCoaTemplateDO> selectEnabledApprovedList() {
        return selectList(new LambdaQueryWrapperX<QmsCoaTemplateDO>()
                .eq(QmsCoaTemplateDO::getStatus, 1)
                .eq(QmsCoaTemplateDO::getAuditStatus, "APPROVED")
                .orderByDesc(QmsCoaTemplateDO::getId));
    }
}
