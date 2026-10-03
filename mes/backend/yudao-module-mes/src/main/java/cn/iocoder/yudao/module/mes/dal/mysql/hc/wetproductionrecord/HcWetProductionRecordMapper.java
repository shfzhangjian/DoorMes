package cn.iocoder.yudao.module.mes.dal.mysql.hc.wetproductionrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.wetproductionrecord.HcWetProductionRecordDO;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcWetProductionRecordMapper extends BaseMapperX<HcWetProductionRecordDO> {

    default PageResult<HcWetProductionRecordDO> selectPage(HcWetProductionRecordPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcWetProductionRecordDO> selectList(HcWetProductionRecordPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcWetProductionRecordDO> buildQuery(HcWetProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcWetProductionRecordDO> query = new LambdaQueryWrapperX<HcWetProductionRecordDO>()
                .geIfPresent(HcWetProductionRecordDO::getRecordDate, reqVO.getRecordDateStart())
                .leIfPresent(HcWetProductionRecordDO::getRecordDate, reqVO.getRecordDateEnd())
                .eqIfPresent(HcWetProductionRecordDO::getGuideClothChanged, reqVO.getGuideClothChanged())
                .likeIfPresent(HcWetProductionRecordDO::getChangeDesc, reqVO.getChangeDesc())
                .eqIfPresent(HcWetProductionRecordDO::getStatus, reqVO.getStatus())
                .orderByDesc(HcWetProductionRecordDO::getRecordDate)
                .orderByDesc(HcWetProductionRecordDO::getId);
        if (StrUtil.isNotBlank(reqVO.getBatchNo())) {
            String batchNo = reqVO.getBatchNo().trim();
            query.and(wrapper -> wrapper.like(HcWetProductionRecordDO::getBatchNo, batchNo)
                    .or()
                    .like(HcWetProductionRecordDO::getPetBatchNo, batchNo)
                    .or()
                    .like(HcWetProductionRecordDO::getGuideClothBatchNo, batchNo));
        }
        return query;
    }

    default HcWetProductionRecordDO selectByBizKey(LocalDate recordDate, String modelCode, String materialCode,
                                                   String batchNo, String petModel, String petBatchNo,
                                                   String guideClothBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcWetProductionRecordDO>()
                .eq(HcWetProductionRecordDO::getRecordDate, recordDate)
                .eq(HcWetProductionRecordDO::getModelCode, modelCode)
                .eq(HcWetProductionRecordDO::getMaterialCode, materialCode)
                .eq(HcWetProductionRecordDO::getBatchNo, batchNo)
                .eq(HcWetProductionRecordDO::getPetModel, petModel)
                .eq(HcWetProductionRecordDO::getPetBatchNo, petBatchNo)
                .eq(HcWetProductionRecordDO::getGuideClothBatchNo, guideClothBatchNo)
                .last("LIMIT 1"));
    }

    default List<HcWetProductionRecordDO> selectByRecordIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcWetProductionRecordDO>().in(HcWetProductionRecordDO::getId, ids));
    }

    @Delete("DELETE FROM mes_hc_wet_production_record WHERE id = #{id} AND status = 'WAIT_CONFIRM'")
    int physicalDeleteById(@Param("id") Long id);

    @Delete({
            "<script>",
            "DELETE FROM mes_hc_wet_production_record",
            "WHERE status = 'WAIT_CONFIRM'",
            "AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int physicalDeleteByIds(@Param("ids") Collection<Long> ids);
}
