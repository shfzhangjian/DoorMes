package cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableLedgerDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcToolingConsumableLedgerMapper extends BaseMapperX<HcToolingConsumableLedgerDO> {

    default PageResult<HcToolingConsumableLedgerDO> selectPage(HcToolingConsumableLedgerPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcToolingConsumableLedgerDO> selectList(HcToolingConsumableLedgerPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default PageResult<HcToolingConsumableLedgerDO> selectBalancePage(HcToolingConsumableLedgerPageReqVO reqVO) {
        return selectPage(reqVO, buildBalanceQuery(reqVO));
    }

    default List<HcToolingConsumableLedgerDO> selectBalanceList(HcToolingConsumableLedgerPageReqVO reqVO) {
        return selectList(buildBalanceQuery(reqVO));
    }

    default HcToolingConsumableLedgerDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<HcToolingConsumableLedgerDO>()
                .eq(HcToolingConsumableLedgerDO::getId, id)
                .eq(HcToolingConsumableLedgerDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default LambdaQueryWrapperX<HcToolingConsumableLedgerDO> buildQuery(HcToolingConsumableLedgerPageReqVO reqVO) {
        LambdaQueryWrapperX<HcToolingConsumableLedgerDO> query = new LambdaQueryWrapperX<HcToolingConsumableLedgerDO>()
                .eqIfPresent(HcToolingConsumableLedgerDO::getConsumableType, reqVO.getConsumableType())
                .likeIfPresent(HcToolingConsumableLedgerDO::getModel, reqVO.getModel())
                .likeIfPresent(HcToolingConsumableLedgerDO::getBatchNo, reqVO.getBatchNo())
                .likeIfPresent(HcToolingConsumableLedgerDO::getErpMaterialCode, reqVO.getErpMaterialCode())
                .likeIfPresent(HcToolingConsumableLedgerDO::getReceiverName, reqVO.getReceiverName())
                .eqIfPresent(HcToolingConsumableLedgerDO::getUsageStatus, reqVO.getUsageStatus())
                .betweenIfPresent(HcToolingConsumableLedgerDO::getReceiveTime,
                        reqVO.getReceiveTimeStart(), reqVO.getReceiveTimeEnd())
                .orderByDesc(HcToolingConsumableLedgerDO::getReceiveTime)
                .orderByDesc(HcToolingConsumableLedgerDO::getId);
        applyProcessFilter(query, reqVO.getProcessCode());
        return query;
    }

    private LambdaQueryWrapperX<HcToolingConsumableLedgerDO> buildBalanceQuery(
            HcToolingConsumableLedgerPageReqVO reqVO) {
        LambdaQueryWrapperX<HcToolingConsumableLedgerDO> query = buildQuery(reqVO);
        query.ne(HcToolingConsumableLedgerDO::getUsageStatus, "RETURNED");
        // 胶板完成后可保留盘点余量，但不能再次作为报工可用耗材。
        query.and(q -> q.ne(HcToolingConsumableLedgerDO::getConsumableType, "GLUE_BOARD")
                .or().ne(HcToolingConsumableLedgerDO::getUsageStatus, "USED_UP"));
        return query;
    }

    private static void applyProcessFilter(LambdaQueryWrapperX<HcToolingConsumableLedgerDO> query,
                                           String processCode) {
        if (StrUtil.isBlank(processCode)) {
            return;
        }
        String normalized = StrUtil.trim(processCode).toUpperCase();
        if ("ADHESIVE".equals(normalized) || "ADHESIVE1".equals(normalized)) {
            query.in(HcToolingConsumableLedgerDO::getProcessCode, List.of("ADHESIVE", "ADHESIVE1"));
            return;
        }
        query.eq(HcToolingConsumableLedgerDO::getProcessCode, StrUtil.trim(processCode));
    }

    default HcToolingConsumableLedgerDO selectOneByImportKey(String consumableType, String processCode,
                                                            String model, String batchNo,
                                                            LocalDateTime receiveTime, String receiverName) {
        return selectOne(new LambdaQueryWrapperX<HcToolingConsumableLedgerDO>()
                .eq(HcToolingConsumableLedgerDO::getConsumableType, consumableType)
                .eq(HcToolingConsumableLedgerDO::getProcessCode, processCode)
                .eqIfPresent(HcToolingConsumableLedgerDO::getModel, model)
                .eq(HcToolingConsumableLedgerDO::getBatchNo, batchNo)
                .eq(HcToolingConsumableLedgerDO::getReceiveTime, receiveTime)
                .eq(HcToolingConsumableLedgerDO::getReceiverName, receiverName)
                .eq(HcToolingConsumableLedgerDO::getDeleted, false)
                .orderByDesc(HcToolingConsumableLedgerDO::getId)
                .last("LIMIT 1"));
    }
}
