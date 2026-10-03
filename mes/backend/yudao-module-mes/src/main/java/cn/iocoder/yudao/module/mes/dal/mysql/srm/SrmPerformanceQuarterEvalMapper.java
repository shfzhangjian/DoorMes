package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceQuarterEvalDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPerformanceQuarterEvalMapper extends BaseMapperX<SrmPerformanceQuarterEvalDO> {

    default PageResult<SrmPerformanceQuarterEvalDO> selectPage(SrmPerformanceQuarterEvaluationPageReqVO reqVO) {
        LambdaQueryWrapperX<SrmPerformanceQuarterEvalDO> query = new LambdaQueryWrapperX<SrmPerformanceQuarterEvalDO>()
                .likeIfPresent(SrmPerformanceQuarterEvalDO::getEvaluationNo, reqVO.getEvaluationNo())
                .eqIfPresent(SrmPerformanceQuarterEvalDO::getSupplierId, reqVO.getSupplierId())
                .likeIfPresent(SrmPerformanceQuarterEvalDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(SrmPerformanceQuarterEvalDO::getSupplierName, reqVO.getSupplierName())
                .eqIfPresent(SrmPerformanceQuarterEvalDO::getEvalYear, reqVO.getEvalYear())
                .eqIfPresent(SrmPerformanceQuarterEvalDO::getEvalQuarter, reqVO.getEvalQuarter())
                .eqIfPresent(SrmPerformanceQuarterEvalDO::getTemplateId, reqVO.getTemplateId())
                .eqIfPresent(SrmPerformanceQuarterEvalDO::getTemplateVersionId, reqVO.getTemplateVersionId())
                .eqIfPresent(SrmPerformanceQuarterEvalDO::getStatus, reqVO.getStatus());
        if (StrUtil.isNotBlank(reqVO.getSupplierInfo())) {
            query.and(q -> q.like(SrmPerformanceQuarterEvalDO::getSupplierCode, reqVO.getSupplierInfo())
                    .or()
                    .like(SrmPerformanceQuarterEvalDO::getSupplierName, reqVO.getSupplierInfo()));
        }
        query.orderByDesc(SrmPerformanceQuarterEvalDO::getUpdateTime)
                .orderByDesc(SrmPerformanceQuarterEvalDO::getId);
        if (reqVO.getVisibleEvaluationIds() != null) {
            if (CollUtil.isEmpty(reqVO.getVisibleEvaluationIds())) {
                query.eq(SrmPerformanceQuarterEvalDO::getId, -1L);
            } else {
                query.in(SrmPerformanceQuarterEvalDO::getId, reqVO.getVisibleEvaluationIds());
            }
        }
        return selectPage(reqVO, query);
    }

    default SrmPerformanceQuarterEvalDO selectByEvaluationNo(String evaluationNo) {
        return selectOne(SrmPerformanceQuarterEvalDO::getEvaluationNo, evaluationNo);
    }

    default SrmPerformanceQuarterEvalDO selectBySupplierAndQuarter(Long supplierId, Integer evalYear,
                                                                   Integer evalQuarter, Long templateVersionId) {
        return selectOne(new LambdaQueryWrapperX<SrmPerformanceQuarterEvalDO>()
                .eq(SrmPerformanceQuarterEvalDO::getSupplierId, supplierId)
                .eq(SrmPerformanceQuarterEvalDO::getEvalYear, evalYear)
                .eq(SrmPerformanceQuarterEvalDO::getEvalQuarter, evalQuarter)
                .eq(SrmPerformanceQuarterEvalDO::getTemplateVersionId, templateVersionId));
    }

    default SrmPerformanceQuarterEvalDO selectBySupplierAndPeriod(Long supplierId, Integer evalYear,
                                                                  Integer evalQuarter) {
        return selectOne(new LambdaQueryWrapperX<SrmPerformanceQuarterEvalDO>()
                .eq(SrmPerformanceQuarterEvalDO::getSupplierId, supplierId)
                .eq(SrmPerformanceQuarterEvalDO::getEvalYear, evalYear)
                .eq(SrmPerformanceQuarterEvalDO::getEvalQuarter, evalQuarter));
    }

    default java.util.List<SrmPerformanceQuarterEvalDO> selectListBySupplierYear(Long supplierId, Integer evalYear) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceQuarterEvalDO>()
                .eq(SrmPerformanceQuarterEvalDO::getSupplierId, supplierId)
                .eq(SrmPerformanceQuarterEvalDO::getEvalYear, evalYear)
                .orderByAsc(SrmPerformanceQuarterEvalDO::getEvalQuarter)
                .orderByDesc(SrmPerformanceQuarterEvalDO::getUpdateTime)
                .orderByDesc(SrmPerformanceQuarterEvalDO::getId));
    }

}
