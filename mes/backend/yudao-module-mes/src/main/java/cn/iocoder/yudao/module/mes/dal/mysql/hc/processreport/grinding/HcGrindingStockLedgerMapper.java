package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingStockLedgerDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcGrindingStockLedgerMapper extends BaseMapperX<HcGrindingStockLedgerDO> {

    default List<HcGrindingStockLedgerDO> selectListByReportId(Long grindingReportId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingStockLedgerDO>()
                .eq(HcGrindingStockLedgerDO::getGrindingReportId, grindingReportId)
                .eq(HcGrindingStockLedgerDO::getDeleted, false)
                .orderByAsc(HcGrindingStockLedgerDO::getId));
    }

    default List<HcGrindingStockLedgerDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingStockLedgerDO>()
                .eq(HcGrindingStockLedgerDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingStockLedgerDO::getDeleted, false)
                .orderByAsc(HcGrindingStockLedgerDO::getId));
    }

    default List<HcGrindingStockLedgerDO> selectListBySecondDetailId(Long secondDetailId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingStockLedgerDO>()
                .eq(HcGrindingStockLedgerDO::getSecondDetailId, secondDetailId)
                .eq(HcGrindingStockLedgerDO::getDeleted, false)
                .orderByAsc(HcGrindingStockLedgerDO::getId));
    }

    default List<HcGrindingStockLedgerDO> selectListBySourceRowUid(Long planOperationId, String sourceRowUid) {
        return selectList(new LambdaQueryWrapperX<HcGrindingStockLedgerDO>()
                .eq(HcGrindingStockLedgerDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingStockLedgerDO::getSourceRowUid, sourceRowUid)
                .eq(HcGrindingStockLedgerDO::getDeleted, false)
                .orderByAsc(HcGrindingStockLedgerDO::getId));
    }

    default List<HcGrindingStockLedgerDO> selectListByLedgerStatus(Long planOperationId, String ledgerStatus) {
        return selectList(new LambdaQueryWrapperX<HcGrindingStockLedgerDO>()
                .eq(HcGrindingStockLedgerDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingStockLedgerDO::getLedgerStatus, ledgerStatus)
                .eq(HcGrindingStockLedgerDO::getDeleted, false)
                .orderByAsc(HcGrindingStockLedgerDO::getId));
    }
}
