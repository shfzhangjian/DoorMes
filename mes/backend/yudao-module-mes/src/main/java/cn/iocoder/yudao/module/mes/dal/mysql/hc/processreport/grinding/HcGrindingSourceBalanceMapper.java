package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSourceBalanceDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcGrindingSourceBalanceMapper extends BaseMapperX<HcGrindingSourceBalanceDO> {

    default HcGrindingSourceBalanceDO selectByBalanceKey(String balanceKey) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingSourceBalanceDO>()
                .eq(HcGrindingSourceBalanceDO::getBalanceKey, balanceKey)
                .eq(HcGrindingSourceBalanceDO::getDeleted, false)
                .orderByDesc(HcGrindingSourceBalanceDO::getId)
                .last("LIMIT 1"));
    }

    default HcGrindingSourceBalanceDO selectByBatchNo(String batchNo) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingSourceBalanceDO>()
                .and(wrapper -> wrapper
                        .eq(HcGrindingSourceBalanceDO::getSourceBatchNo, batchNo)
                        .or()
                        .eq(HcGrindingSourceBalanceDO::getSourceProductionBatchNo, batchNo))
                .eq(HcGrindingSourceBalanceDO::getDeleted, false)
                .orderByDesc(HcGrindingSourceBalanceDO::getId)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT *
            FROM mes_sfc_grinding_source_balance
            WHERE deleted = 0
              AND (source_batch_no = #{batchNo} OR source_production_batch_no = #{batchNo})
            ORDER BY id DESC
            LIMIT 1
            FOR UPDATE
            """)
    HcGrindingSourceBalanceDO selectByBatchNoForUpdate(@Param("batchNo") String batchNo);

    default List<HcGrindingSourceBalanceDO> selectAvailableList(String sourceType) {
        return selectList(new LambdaQueryWrapperX<HcGrindingSourceBalanceDO>()
                .eqIfPresent(HcGrindingSourceBalanceDO::getSourceType, sourceType)
                .eq(HcGrindingSourceBalanceDO::getStatus, "AVAILABLE")
                .gt(HcGrindingSourceBalanceDO::getAvailableLength, java.math.BigDecimal.ZERO)
                .eq(HcGrindingSourceBalanceDO::getDeleted, false)
                .orderByDesc(HcGrindingSourceBalanceDO::getLastReportTime)
                .orderByDesc(HcGrindingSourceBalanceDO::getId));
    }
}
