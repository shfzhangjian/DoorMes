package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slittingpress;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slittingpress.HcSlittingPressProductionRecordDO;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcSlittingPressProductionLedgerMapper extends BaseMapperX<HcSlittingPressProductionRecordDO> {
    default PageResult<HcSlittingPressProductionRecordDO> selectPage(HcSlittingPressProductionRecordPageReqVO req) {
        return selectPage(req, query(req));
    }
    default List<HcSlittingPressProductionRecordDO> selectList(HcSlittingPressProductionRecordPageReqVO req) {
        return selectList(query(req));
    }
    default LambdaQueryWrapperX<HcSlittingPressProductionRecordDO> query(HcSlittingPressProductionRecordPageReqVO req) {
        return new LambdaQueryWrapperX<HcSlittingPressProductionRecordDO>()
                .geIfPresent(HcSlittingPressProductionRecordDO::getReportDate, req.getReportDateStart())
                .leIfPresent(HcSlittingPressProductionRecordDO::getReportDate, req.getReportDateEnd())
                .likeIfPresent(HcSlittingPressProductionRecordDO::getModelCode, req.getModelCode())
                .likeIfPresent(HcSlittingPressProductionRecordDO::getMaterialCode, req.getMaterialCode())
                .likeIfPresent(HcSlittingPressProductionRecordDO::getBatchNo, req.getBatchNo())
                .likeIfPresent(HcSlittingPressProductionRecordDO::getRecorderName, req.getRecorderName())
                .eqIfPresent(HcSlittingPressProductionRecordDO::getStatus, req.getStatus())
                .orderByDesc(HcSlittingPressProductionRecordDO::getReportDate)
                .orderByDesc(HcSlittingPressProductionRecordDO::getId);
    }
    default HcSlittingPressProductionRecordDO selectByBizKey(LocalDate date, String model, String material, String batch) {
        return selectOne(new LambdaQueryWrapperX<HcSlittingPressProductionRecordDO>()
                .eq(HcSlittingPressProductionRecordDO::getReportDate, date)
                .eq(HcSlittingPressProductionRecordDO::getModelCode, model)
                .eq(HcSlittingPressProductionRecordDO::getMaterialCode, material)
                .eq(HcSlittingPressProductionRecordDO::getBatchNo, batch).last("LIMIT 1"));
    }
    default List<HcSlittingPressProductionRecordDO> selectByRecordIds(Collection<Long> ids) {
        return ids == null || ids.isEmpty() ? List.of() : selectList(new LambdaQueryWrapperX<HcSlittingPressProductionRecordDO>()
                .in(HcSlittingPressProductionRecordDO::getId, ids));
    }
    @Delete("DELETE FROM mes_hc_slitting_press_production_record WHERE id = #{id} AND status = 'WAIT_CONFIRM'")
    int physicalDeleteById(@Param("id") Long id);
    @Delete({"<script>", "DELETE FROM mes_hc_slitting_press_production_record WHERE status = 'WAIT_CONFIRM' AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>", "</script>"})
    int physicalDeleteByIds(@Param("ids") Collection<Long> ids);
}
