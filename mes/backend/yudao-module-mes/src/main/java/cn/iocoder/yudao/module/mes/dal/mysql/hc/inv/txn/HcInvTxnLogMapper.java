package cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.txn;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.txn.vo.HcInvTxnLogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgHistoryLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgHistoryLedgerRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryPieceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HcInvTxnLogMapper extends BaseMapperX<HcInvTxnLogDO> {

    default PageResult<HcInvTxnLogDO> selectPage(HcInvTxnLogPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcInvTxnLogDO> selectList(HcInvTxnLogPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    /**
     * 分切压槽不合格品历史台账。
     *
     * <p>以库存交易流水为一行事实，通过 {@code source_id} 关联逐片台账补齐片号、工序和当前最终状态。
     * 当前状态仅用于筛选和展示，不可被当作交易发生时的状态。</p>
     */
    default PageResult<NgHistoryLedgerRespVO> selectNgHistoryLedgerPage(NgHistoryLedgerPageReqVO reqVO) {
        MPJLambdaWrapperX<HcInvTxnLogDO> query = new MPJLambdaWrapperX<HcInvTxnLogDO>()
                .selectAll(HcInvTxnLogDO.class)
                .selectAs(HcNgInventoryPieceDO::getPieceNo, NgHistoryLedgerRespVO::getPieceNo)
                .selectAs(HcNgInventoryPieceDO::getSourcePlanNo, NgHistoryLedgerRespVO::getSourcePlanNo)
                .selectAs(HcNgInventoryPieceDO::getSourceParentBatchNo, NgHistoryLedgerRespVO::getSourceParentBatchNo)
                .selectAs(HcNgInventoryPieceDO::getProcessType, NgHistoryLedgerRespVO::getProcessType)
                .selectAs(HcNgInventoryPieceDO::getProcessName, NgHistoryLedgerRespVO::getProcessName)
                .selectAs(HcNgInventoryPieceDO::getPadType, NgHistoryLedgerRespVO::getPadType)
                .selectAs(HcNgInventoryPieceDO::getStatus, NgHistoryLedgerRespVO::getCurrentStatus)
                .selectAs(HcNgInventoryPieceDO::getScrapReason, NgHistoryLedgerRespVO::getScrapReason)
                .leftJoin(HcNgInventoryPieceDO.class, HcNgInventoryPieceDO::getId, HcInvTxnLogDO::getSourceId)
                .eq(HcInvTxnLogDO::getSourceType, "NG_INVENTORY")
                .eq(HcInvTxnLogDO::getSourceTable, "mes_inv_ng_piece")
                .eq(HcNgInventoryPieceDO::getDeleted, false)
                .eqIfPresent(HcInvTxnLogDO::getWarehouseCode, reqVO.getWarehouseCode())
                .eqIfPresent(HcNgInventoryPieceDO::getProcessType, reqVO.getProcessType())
                .eqIfPresent(HcInvTxnLogDO::getTxnType, reqVO.getTxnType())
                .eqIfPresent(HcNgInventoryPieceDO::getStatus, reqVO.getCurrentStatus())
                .likeIfPresent(HcInvTxnLogDO::getModelNo, reqVO.getModelNo())
                .likeIfPresent(HcInvTxnLogDO::getMaterialCode, reqVO.getMaterialCode())
                .eqIfPresent(HcNgInventoryPieceDO::getPadType, reqVO.getPadType())
                .geIfPresent(HcInvTxnLogDO::getTxnTime, reqVO.getTxnTimeStart())
                .leIfPresent(HcInvTxnLogDO::getTxnTime, reqVO.getTxnTimeEnd())
                .orderByDesc(HcInvTxnLogDO::getTxnTime)
                .orderByDesc(HcInvTxnLogDO::getId);
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            query.and(wrapper -> wrapper.like(HcNgInventoryPieceDO::getPieceNo, reqVO.getKeyword())
                    .or().like(HcInvTxnLogDO::getSourceBatchNo, reqVO.getKeyword())
                    .or().like(HcNgInventoryPieceDO::getSourceParentBatchNo, reqVO.getKeyword())
                    .or().like(HcNgInventoryPieceDO::getSourcePlanNo, reqVO.getKeyword()));
        }
        if (Boolean.TRUE.equals(reqVO.getArchivedOnly())) {
            query.notIn(HcNgInventoryPieceDO::getStatus, List.of("STORED", "FROZEN"));
        }
        return selectJoinPage(reqVO, NgHistoryLedgerRespVO.class, query);
    }

    default LambdaQueryWrapperX<HcInvTxnLogDO> buildQuery(HcInvTxnLogPageReqVO reqVO) {
        return new LambdaQueryWrapperX<HcInvTxnLogDO>()
                .eqIfPresent(HcInvTxnLogDO::getWarehouseCode, reqVO.getWarehouseCode())
                .likeIfPresent(HcInvTxnLogDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(HcInvTxnLogDO::getModelNo, reqVO.getModelNo())
                .likeIfPresent(HcInvTxnLogDO::getBatchNo, reqVO.getBatchNo())
                .inIfPresent(HcInvTxnLogDO::getTxnType, reqVO.getTxnTypes())
                .eqIfPresent(HcInvTxnLogDO::getTxnNo, reqVO.getTxnNo())
                .geIfPresent(HcInvTxnLogDO::getTxnTime, reqVO.getTxnTimeStart())
                .leIfPresent(HcInvTxnLogDO::getTxnTime, reqVO.getTxnTimeEnd())
                .orderByDesc(HcInvTxnLogDO::getTxnTime);
    }

}
