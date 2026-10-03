package cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableConsumeDO;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcToolingConsumableConsumeMapper extends BaseMapperX<HcToolingConsumableConsumeDO> {

    default PageResult<HcToolingConsumableConsumeDO> selectPage(HcToolingConsumableConsumePageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcToolingConsumableConsumeDO> selectListByLedgerId(Long ledgerId) {
        return selectList(new LambdaQueryWrapperX<HcToolingConsumableConsumeDO>()
                .eq(HcToolingConsumableConsumeDO::getLedgerId, ledgerId)
                .eq(HcToolingConsumableConsumeDO::getDeleted, false)
                .orderByDesc(HcToolingConsumableConsumeDO::getConsumeTime)
                .orderByDesc(HcToolingConsumableConsumeDO::getId));
    }

    default List<HcToolingConsumableConsumeDO> selectListByLedgerIds(Collection<Long> ledgerIds) {
        if (ledgerIds == null || ledgerIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcToolingConsumableConsumeDO>()
                .in(HcToolingConsumableConsumeDO::getLedgerId, ledgerIds)
                .eq(HcToolingConsumableConsumeDO::getDeleted, false)
                .orderByAsc(HcToolingConsumableConsumeDO::getLedgerId)
                .orderByDesc(HcToolingConsumableConsumeDO::getConsumeTime)
                .orderByDesc(HcToolingConsumableConsumeDO::getId));
    }

    default void deleteByLedgerId(Long ledgerId) {
        delete(new LambdaQueryWrapperX<HcToolingConsumableConsumeDO>()
                .eq(HcToolingConsumableConsumeDO::getLedgerId, ledgerId));
    }

    default void deleteByLedgerIds(Collection<Long> ledgerIds) {
        if (ledgerIds == null || ledgerIds.isEmpty()) {
            return;
        }
        delete(new LambdaQueryWrapperX<HcToolingConsumableConsumeDO>()
                .in(HcToolingConsumableConsumeDO::getLedgerId, ledgerIds));
    }

    default BigDecimal sumConsumeQtyByLedgerId(Long ledgerId, Long excludeId) {
        LambdaQueryWrapperX<HcToolingConsumableConsumeDO> query = new LambdaQueryWrapperX<>();
        query.eq(HcToolingConsumableConsumeDO::getLedgerId, ledgerId)
                .eq(HcToolingConsumableConsumeDO::getDeleted, false);
        if (excludeId != null) {
            query.ne(HcToolingConsumableConsumeDO::getId, excludeId);
        }
        return selectList(query).stream()
                .map(HcToolingConsumableConsumeDO::getConsumeQty)
                .filter(qty -> qty != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default List<HcToolingConsumableConsumeDO> selectListForUpdate(Long ledgerId) {
        return selectList(new LambdaQueryWrapperX<HcToolingConsumableConsumeDO>()
                .eq(HcToolingConsumableConsumeDO::getLedgerId, ledgerId).last("FOR UPDATE"));
    }

    default BigDecimal sumConsumeQtyForUpdate(Long ledgerId) {
        return selectListForUpdate(ledgerId).stream().map(HcToolingConsumableConsumeDO::getConsumeQty)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 汇总直接关联到胶板报工领用记录的台账消耗。
     *
     * 台账消耗与粘胶1/粘胶2报工消耗属于两类独立业务记录，回算胶板余量时必须同时纳入。
     */
    default BigDecimal sumConsumeQtyByGlueBoardUsageId(Long glueBoardUsageId) {
        if (glueBoardUsageId == null) {
            return BigDecimal.ZERO;
        }
        return selectList(new LambdaQueryWrapperX<HcToolingConsumableConsumeDO>()
                .eq(HcToolingConsumableConsumeDO::getGlueBoardUsageId, glueBoardUsageId)
                .eq(HcToolingConsumableConsumeDO::getDeleted, false))
                .stream()
                .map(HcToolingConsumableConsumeDO::getConsumeQty)
                .filter(qty -> qty != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default LambdaQueryWrapperX<HcToolingConsumableConsumeDO> buildQuery(HcToolingConsumableConsumePageReqVO reqVO) {
        LambdaQueryWrapperX<HcToolingConsumableConsumeDO> query = new LambdaQueryWrapperX<HcToolingConsumableConsumeDO>()
                .eqIfPresent(HcToolingConsumableConsumeDO::getLedgerId, reqVO.getLedgerId())
                .eqIfPresent(HcToolingConsumableConsumeDO::getConsumableType, reqVO.getConsumableType())
                .likeIfPresent(HcToolingConsumableConsumeDO::getModel, reqVO.getModel())
                .likeIfPresent(HcToolingConsumableConsumeDO::getBatchNo, reqVO.getBatchNo())
                .likeIfPresent(HcToolingConsumableConsumeDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcToolingConsumableConsumeDO::getProductionBatchNo, reqVO.getProductionBatchNo())
                .likeIfPresent(HcToolingConsumableConsumeDO::getProductModelCode, reqVO.getProductModelCode())
                .likeIfPresent(HcToolingConsumableConsumeDO::getProductMaterialCode, reqVO.getProductMaterialCode())
                .likeIfPresent(HcToolingConsumableConsumeDO::getProductBatchNo, reqVO.getProductBatchNo())
                .betweenIfPresent(HcToolingConsumableConsumeDO::getConsumeTime,
                        reqVO.getConsumeTimeStart(), reqVO.getConsumeTimeEnd())
                .betweenIfPresent(HcToolingConsumableConsumeDO::getCreateTime,
                        reqVO.getCreateTimeStart(), reqVO.getCreateTimeEnd())
                .orderByDesc(HcToolingConsumableConsumeDO::getConsumeTime)
                .orderByDesc(HcToolingConsumableConsumeDO::getId);
        String processCode = StrUtil.trimToEmpty(reqVO.getProcessCode()).toUpperCase();
        if ("ADHESIVE".equals(processCode) || "ADHESIVE1".equals(processCode)) {
            query.in(HcToolingConsumableConsumeDO::getProcessCode, List.of("ADHESIVE", "ADHESIVE1"));
        } else if (StrUtil.isNotBlank(processCode)) {
            query.eq(HcToolingConsumableConsumeDO::getProcessCode, processCode);
        }
        return query;
    }
}
