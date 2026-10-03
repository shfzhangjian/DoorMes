package cn.iocoder.yudao.module.mes.dal.mysql.hc.lotinstance;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotinstance.vo.HcLotInstancePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotinstance.HcLotInstanceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcLotInstanceMapper extends BaseMapperX<HcLotInstanceDO> {

    default PageResult<HcLotInstanceDO> selectPage(HcLotInstancePageReqVO reqVO) {
        LambdaQueryWrapperX<HcLotInstanceDO> query = new LambdaQueryWrapperX<HcLotInstanceDO>()
                .eqIfPresent(HcLotInstanceDO::getRuleId, reqVO.getRuleId())
                .eqIfPresent(HcLotInstanceDO::getRuleCode, reqVO.getRuleCode())
                .eqIfPresent(HcLotInstanceDO::getProductCategoryCode, reqVO.getProductCategoryCode())
                .eqIfPresent(HcLotInstanceDO::getProdType, reqVO.getProdType())
                .likeIfPresent(HcLotInstanceDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(HcLotInstanceDO::getModelCode, reqVO.getModelCode())
                .eqIfPresent(HcLotInstanceDO::getBatchLevel, reqVO.getBatchLevel())
                .eqIfPresent(HcLotInstanceDO::getInstanceStatus, reqVO.getInstanceStatus())
                .eqIfPresent(HcLotInstanceDO::getGenerateSource, reqVO.getGenerateSource())
                .eqIfPresent(HcLotInstanceDO::getLineCode, reqVO.getLineCode());
        if (StrUtil.isNotBlank(reqVO.getLotNo())) {
            query.and(wrapper -> wrapper.like(HcLotInstanceDO::getLotNo, reqVO.getLotNo())
                    .or()
                    .like(HcLotInstanceDO::getProductionBatchNo, reqVO.getLotNo()));
        }
        if (StrUtil.isNotBlank(reqVO.getMaterialKeyword())) {
            query.and(wrapper -> wrapper.like(HcLotInstanceDO::getMaterialCode, reqVO.getMaterialKeyword())
                    .or()
                    .like(HcLotInstanceDO::getMaterialName, reqVO.getMaterialKeyword()));
        }
        return selectPage(reqVO, query
                .orderByDesc(HcLotInstanceDO::getGeneratedTime)
                .orderByDesc(HcLotInstanceDO::getId));
    }

    default Long selectCountByRuleId(Long ruleId) {
        return selectCount(new LambdaQueryWrapperX<HcLotInstanceDO>()
                .eq(HcLotInstanceDO::getRuleId, ruleId));
    }

    default HcLotInstanceDO selectByLotNo(String lotNo) {
        return selectOne(new LambdaQueryWrapperX<HcLotInstanceDO>()
                .eq(HcLotInstanceDO::getLotNo, lotNo)
                .last("LIMIT 1"));
    }

    default HcLotInstanceDO selectByIdempotentKey(String idempotentKey) {
        return selectOne(new LambdaQueryWrapperX<HcLotInstanceDO>()
                .eq(HcLotInstanceDO::getIdempotentKey, idempotentKey)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT COALESCE(MAX(annual_batch_seq), 0)
            FROM mes_lot_instance
            WHERE deleted = 0
              AND rule_id = #{ruleId}
              AND annual_batch_seq IS NOT NULL
              AND (
                (biz_date IS NOT NULL AND YEAR(biz_date) = #{year})
                OR year_code = #{yearCode}
              )
            """)
    Integer selectMaxAnnualBatchSeqByRuleAndYear(@Param("ruleId") Long ruleId,
                                                 @Param("year") Integer year,
                                                 @Param("yearCode") String yearCode);
}
