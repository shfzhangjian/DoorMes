package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundProductionRecordDO;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcCutRoundProductionRecordMapper extends BaseMapperX<HcCutRoundProductionRecordDO> {

    default PageResult<HcCutRoundProductionRecordDO> selectPage(HcCutRoundProductionRecordPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<HcCutRoundProductionRecordDO> selectList(HcCutRoundProductionRecordPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<HcCutRoundProductionRecordDO> buildQuery(
            HcCutRoundProductionRecordPageReqVO reqVO) {
        LambdaQueryWrapperX<HcCutRoundProductionRecordDO> wrapper = new LambdaQueryWrapperX<HcCutRoundProductionRecordDO>()
                .geIfPresent(HcCutRoundProductionRecordDO::getReportDate, reqVO.getReportDateStart())
                .leIfPresent(HcCutRoundProductionRecordDO::getReportDate, reqVO.getReportDateEnd())
                .likeIfPresent(HcCutRoundProductionRecordDO::getModelCode, reqVO.getModelCode())
                .likeIfPresent(HcCutRoundProductionRecordDO::getProductionBatchNo, reqVO.getProductionBatchNo())
                .eqIfPresent(HcCutRoundProductionRecordDO::getCutSizeMm, normalizeCutSize(reqVO.getCutSizeMm()))
                .likeIfPresent(HcCutRoundProductionRecordDO::getRecorderName, reqVO.getRecorderName())
                .eqIfPresent(HcCutRoundProductionRecordDO::getStatus, reqVO.getStatus());
        if ("UNCLASSIFIED".equals(reqVO.getPadType())) {
            wrapper.isNull(HcCutRoundProductionRecordDO::getPadType);
        } else {
            wrapper.eqIfPresent(HcCutRoundProductionRecordDO::getPadType, reqVO.getPadType());
        }
        return wrapper.orderByDesc(HcCutRoundProductionRecordDO::getReportDate)
                .orderByDesc(HcCutRoundProductionRecordDO::getId);
    }

    default HcCutRoundProductionRecordDO selectByBizKey(LocalDate reportDate, String modelCode,
                                                          String productionBatchNo, String cutSizeMm) {
        return selectOne(new LambdaQueryWrapperX<HcCutRoundProductionRecordDO>()
                .eq(HcCutRoundProductionRecordDO::getReportDate, reportDate)
                .eq(HcCutRoundProductionRecordDO::getModelCode, modelCode)
                .eq(HcCutRoundProductionRecordDO::getProductionBatchNo, productionBatchNo)
                .eq(HcCutRoundProductionRecordDO::getCutSizeMm, normalizeCutSize(cutSizeMm))
                .last("LIMIT 1"));
    }

    default List<HcCutRoundProductionRecordDO> selectByRecordIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcCutRoundProductionRecordDO>()
                .in(HcCutRoundProductionRecordDO::getId, ids));
    }

    @Delete("DELETE FROM mes_hc_cut_round_production_record WHERE id = #{id} AND status = 'WAIT_CONFIRM'")
    int physicalDeleteById(@Param("id") Long id);

    @Delete({
            "<script>",
            "DELETE FROM mes_hc_cut_round_production_record",
            "WHERE status = 'WAIT_CONFIRM'",
            "AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int physicalDeleteByIds(@Param("ids") Collection<Long> ids);

    private static String normalizeCutSize(String value) {
        String text = StrUtil.trimToNull(value);
        if (text == null) {
            return null;
        }
        return text.toLowerCase().endsWith("mm") ? text.substring(0, text.length() - 2).trim() : text;
    }
}
