package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCmpWarpageSliceStatPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsCmpWarpageSliceStatDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QmsCmpWarpageSliceStatMapper extends BaseMapperX<QmsCmpWarpageSliceStatDO> {

    default PageResult<QmsCmpWarpageSliceStatDO> selectPage(QmsCmpWarpageSliceStatPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO)
                .orderByDesc(QmsCmpWarpageSliceStatDO::getRecordDate)
                .orderByAsc(QmsCmpWarpageSliceStatDO::getModelCode)
                .orderByAsc(QmsCmpWarpageSliceStatDO::getParentBatchNo)
                .orderByAsc(QmsCmpWarpageSliceStatDO::getSegmentSliceNo)
                .orderByAsc(QmsCmpWarpageSliceStatDO::getProductionSliceNo)
                .orderByDesc(QmsCmpWarpageSliceStatDO::getId));
    }

    default QmsCmpWarpageSliceStatDO selectByProductionSliceNo(String productionSliceNo) {
        return selectOne(new LambdaQueryWrapperX<QmsCmpWarpageSliceStatDO>()
                .eq(QmsCmpWarpageSliceStatDO::getProductionSliceNo, productionSliceNo)
                .eq(QmsCmpWarpageSliceStatDO::getDeleted, false)
                .orderByDesc(QmsCmpWarpageSliceStatDO::getId)
                .last("LIMIT 1"));
    }

    private LambdaQueryWrapperX<QmsCmpWarpageSliceStatDO> buildQuery(QmsCmpWarpageSliceStatPageReqVO reqVO) {
        LambdaQueryWrapperX<QmsCmpWarpageSliceStatDO> query = new LambdaQueryWrapperX<QmsCmpWarpageSliceStatDO>()
                .likeIfPresent(QmsCmpWarpageSliceStatDO::getModelCode, reqVO.getModelCode())
                .likeIfPresent(QmsCmpWarpageSliceStatDO::getParentBatchNo, reqVO.getParentBatchNo())
                .likeIfPresent(QmsCmpWarpageSliceStatDO::getSegmentSliceNo, reqVO.getSegmentSliceNo())
                .eqIfPresent(QmsCmpWarpageSliceStatDO::getManualOverride, reqVO.getManualOverride())
                .geIfPresent(QmsCmpWarpageSliceStatDO::getRecordDate, reqVO.getRecordDateStart())
                .leIfPresent(QmsCmpWarpageSliceStatDO::getRecordDate, reqVO.getRecordDateEnd());
        if ("PENDING".equals(reqVO.getInspectionResult())) {
            // 与列表显示一致：没有 OK/NG 结论的记录均为待判定，兼容历史空值。
            query.and(wrapper -> wrapper.isNull(QmsCmpWarpageSliceStatDO::getInspectionResult)
                    .or().notIn(QmsCmpWarpageSliceStatDO::getInspectionResult, "OK", "NG"));
        } else {
            query.eqIfPresent(QmsCmpWarpageSliceStatDO::getInspectionResult, reqVO.getInspectionResult());
        }
        query.and(reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank(), wrapper -> wrapper
                        .like(QmsCmpWarpageSliceStatDO::getProductionSliceNo, reqVO.getKeyword())
                        .or().like(QmsCmpWarpageSliceStatDO::getParentBatchNo, reqVO.getKeyword())
                        .or().like(QmsCmpWarpageSliceStatDO::getSegmentSliceNo, reqVO.getKeyword())
                        .or().like(QmsCmpWarpageSliceStatDO::getCustomerSliceNo, reqVO.getKeyword())
                        .or().like(QmsCmpWarpageSliceStatDO::getShippingNoticeNo, reqVO.getKeyword()));
        query.eq(QmsCmpWarpageSliceStatDO::getDeleted, false);
        return query;
    }
}
