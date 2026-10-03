package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo.HcInventoryAnalysisOverviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockHistoryLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFinishedStockTxnLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/** 成品库存历史流水 Mapper。 */
@Mapper
public interface HcFinishedStockTxnLogMapper extends BaseMapperX<HcFinishedStockTxnLogDO> {

    /**
     * 成品库位库存分析的必要流水。
     *
     * <p>库位范围仍由调用方使用有效 {@code PACKAGE_FG} 库位编码二次限制，避免历史无效库位进入报表。</p>
     */
    default List<HcFinishedStockTxnLogDO> selectInventoryAnalysisTxnList(HcInventoryAnalysisOverviewReqVO reqVO) {
        LocalDateTime startTime = reqVO.getStartDate().atStartOfDay();
        LocalDateTime endTimeExclusive = reqVO.getEndDate().plusDays(1).atStartOfDay();
        return selectList(new LambdaQueryWrapperX<HcFinishedStockTxnLogDO>()
                .in(HcFinishedStockTxnLogDO::getTxnType, List.of(
                        "FG_INBOUND", "FG_MANUAL_OUTBOUND", "FG_SHIP",
                        "FG_PACKAGE_SPLIT_RETURN", "FG_SHIPPING_RETURN"))
                .ge(HcFinishedStockTxnLogDO::getTxnTime, startTime)
                .lt(HcFinishedStockTxnLogDO::getTxnTime, endTimeExclusive)
                .eq(HcFinishedStockTxnLogDO::getDeleted, false)
                .orderByAsc(HcFinishedStockTxnLogDO::getTxnTime)
                .orderByAsc(HcFinishedStockTxnLogDO::getId));
    }

    default PageResult<HcFinishedStockTxnLogDO> selectHistoryLedgerPage(FgStockHistoryLedgerPageReqVO reqVO) {
        LambdaQueryWrapperX<HcFinishedStockTxnLogDO> query = new LambdaQueryWrapperX<HcFinishedStockTxnLogDO>()
                .eqIfPresent(HcFinishedStockTxnLogDO::getTxnType, reqVO.getTxnType())
                .eqIfPresent(HcFinishedStockTxnLogDO::getAfterStockStatus, reqVO.getAfterStockStatus())
                .eqIfPresent(HcFinishedStockTxnLogDO::getQualityStatus, reqVO.getQualityStatus())
                .likeIfPresent(HcFinishedStockTxnLogDO::getWarehouseCode, reqVO.getWarehouseCode())
                .likeIfPresent(HcFinishedStockTxnLogDO::getLocationCode, reqVO.getLocationCode())
                .likeIfPresent(HcFinishedStockTxnLogDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(HcFinishedStockTxnLogDO::getModelCode, reqVO.getModelCode())
                .likeIfPresent(HcFinishedStockTxnLogDO::getBatchNo, reqVO.getBatchNo())
                .likeIfPresent(HcFinishedStockTxnLogDO::getSliceBatchNo, reqVO.getSliceBatchNo())
                .geIfPresent(HcFinishedStockTxnLogDO::getTxnTime, reqVO.getTxnTimeStart())
                .leIfPresent(HcFinishedStockTxnLogDO::getTxnTime, reqVO.getTxnTimeEnd())
                .orderByDesc(HcFinishedStockTxnLogDO::getTxnTime)
                .orderByDesc(HcFinishedStockTxnLogDO::getId);
        String keyword = StrUtil.trimToNull(reqVO.getKeyword());
        if (keyword != null) {
            query.and(wrapper -> wrapper.like(HcFinishedStockTxnLogDO::getStockNo, keyword)
                    .or().like(HcFinishedStockTxnLogDO::getInnerUnitNo, keyword)
                    .or().like(HcFinishedStockTxnLogDO::getOuterBoxNo, keyword)
                    .or().like(HcFinishedStockTxnLogDO::getSliceBatchNo, keyword)
                    .or().like(HcFinishedStockTxnLogDO::getBatchNo, keyword)
                    .or().like(HcFinishedStockTxnLogDO::getRefDocNo, keyword));
        }
        return selectPage(reqVO, query);
    }
}
