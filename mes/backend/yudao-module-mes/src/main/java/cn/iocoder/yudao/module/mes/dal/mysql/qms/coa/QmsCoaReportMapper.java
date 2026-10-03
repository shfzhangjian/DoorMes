package cn.iocoder.yudao.module.mes.dal.mysql.qms.coa;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.coa.vo.QmsCoaVO.ReportPageReq;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaReportDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

@Mapper
public interface QmsCoaReportMapper extends BaseMapperX<QmsCoaReportDO> {

    default PageResult<QmsCoaReportDO> selectPage(ReportPageReq reqVO) {
        LambdaQueryWrapperX<QmsCoaReportDO> wrapper = new LambdaQueryWrapperX<QmsCoaReportDO>()
                .likeIfPresent(QmsCoaReportDO::getCustomerName, reqVO.getCustomerName())
                .likeIfPresent(QmsCoaReportDO::getShippingNoticeNo, reqVO.getShippingNoticeNo())
                .likeIfPresent(QmsCoaReportDO::getProductionBatchNo, reqVO.getProductionBatchNo())
                .likeIfPresent(QmsCoaReportDO::getProductModelCode, reqVO.getProductModelCode())
                .eqIfPresent(QmsCoaReportDO::getReportStatus, reqVO.getReportStatus());
        if ("PENDING".equalsIgnoreCase(reqVO.getReviewQueue())) {
            wrapper.in(QmsCoaReportDO::getReportStatus,
                    List.of("PENDING_CONFIRM", "PENDING_REVIEW", "APPROVED"));
        }
        if (StringUtils.hasText(reqVO.getKeyword())) {
            wrapper.and(q -> q.like(QmsCoaReportDO::getCoaNo, reqVO.getKeyword())
                    .or().like(QmsCoaReportDO::getMaterialCode, reqVO.getKeyword())
                    .or().like(QmsCoaReportDO::getMaterialName, reqVO.getKeyword())
                    .or().like(QmsCoaReportDO::getCustomerBatchNo, reqVO.getKeyword())
                    .or().like(QmsCoaReportDO::getCustomerProductCode, reqVO.getKeyword()));
        }
        return selectPage(reqVO, wrapper.orderByDesc(QmsCoaReportDO::getId));
    }

    default QmsCoaReportDO selectLatestByRootId(Long rootReportId) {
        return selectOne(new LambdaQueryWrapperX<QmsCoaReportDO>()
                .eq(QmsCoaReportDO::getRootReportId, rootReportId)
                .orderByDesc(QmsCoaReportDO::getRevisionNo)
                .last("LIMIT 1"));
    }

    default List<QmsCoaReportDO> selectListByShippingNoticeId(Long shippingNoticeId) {
        if (shippingNoticeId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<QmsCoaReportDO>()
                .eq(QmsCoaReportDO::getShippingNoticeId, shippingNoticeId)
                .eq(QmsCoaReportDO::getDeleted, false)
                .orderByAsc(QmsCoaReportDO::getId));
    }
}
