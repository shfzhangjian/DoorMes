package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.HcAdhesiveProductionRecordController;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.HcCutRoundProductionRecordController;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.HcSlittingPressProductionRecordController;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HcDailyProductionRecordQueryTest {
    @Test
    void shouldFilterSlittingByOriginalBusinessTimeUsingExclusiveDayEnd() {
        initTable(HcSlittingSliceRecordDO.class);
        HcSlittingSliceRecordMapper mapper = mock(HcSlittingSliceRecordMapper.class, CALLS_REAL_METHODS);
        LocalDate day = LocalDate.of(2026, 9, 24);
        doAnswer(call -> {
            AbstractWrapper<?, ?, ?> query = call.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("COALESCE(source_consume_time, scan_time) >="));
            assertTrue(sql.contains("COALESCE(source_consume_time, scan_time) <"));
            assertTrue(sql.contains("scan_status ="));
            assertTrue(sql.contains("deleted ="));
            assertTrue(query.getParamNameValuePairs().containsValue("CONFIRMED"));
            assertTrue(query.getParamNameValuePairs().containsValue(day.atStartOfDay()));
            assertTrue(query.getParamNameValuePairs().containsValue(day.plusDays(1).atStartOfDay()));
            return List.of();
        }).when(mapper).selectList(any(Wrapper.class));
        HcSlittingPressProductionRecordPageReqVO req = new HcSlittingPressProductionRecordPageReqVO();
        req.setReportDateStart(day);
        req.setReportDateEnd(day);
        mapper.selectProductionRecordSlices(req);
    }

    @Test
    void shouldUseSameFallbackDateForCutRoundQueryAndDisplay() {
        initTable(HcCutRoundReportDO.class);
        HcCutRoundReportMapper mapper = mock(HcCutRoundReportMapper.class, CALLS_REAL_METHODS);
        LocalDate day = LocalDate.of(2026, 9, 24);
        doAnswer(call -> {
            AbstractWrapper<?, ?, ?> query = call.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("COALESCE(report_date, DATE(confirmer_time), DATE(end_time), DATE(recorder_time)) >="));
            assertTrue(query.getParamNameValuePairs().containsValue(day));
            assertTrue(query.getParamNameValuePairs().containsValue("CONFIRMED"));
            assertTrue(query.getParamNameValuePairs().containsValue("SUBMITTED"));
            assertFalse(query.getParamNameValuePairs().containsValue("DRAFT"));
            return List.of();
        }).when(mapper).selectList(any(Wrapper.class));
        HcCutRoundProductionRecordPageReqVO req = new HcCutRoundProductionRecordPageReqVO();
        req.setReportDateStart(day);
        req.setReportDateEnd(day);
        mapper.selectProductionRecordList(req);
    }

    @Test
    void shouldExportRealTimestampWithoutChangingDailyAttribution() {
        assertExportTime(LocalDate.of(2026, 9, 25).atTime(8, 12, 34), "2026-09-25 08:12:34");
    }

    @Test
    void shouldMarkMissingTimeInsteadOfInventingMidnight() {
        assertExportTime(null, "2026-09-24（未记录具体时间）");
        HcPressSlotReportDO report = HcPressSlotReportDO.builder()
                .reportDate(LocalDate.of(2026, 9, 24)).build();
        assertNull(ReflectionTestUtils.invokeMethod(new HcSlittingPressProductionRecordServiceImpl(),
                "resolveReportTime", report));
    }

    private void assertExportTime(LocalDateTime time, String expected) {
        LocalDate businessDate = LocalDate.of(2026, 9, 24);
        HcAdhesiveProductionRecordRespVO adhesive = new HcAdhesiveProductionRecordRespVO();
        adhesive.setReportDate(businessDate);
        adhesive.setRecordTime(time);
        HcSlittingPressProductionRecordRespVO slitting = new HcSlittingPressProductionRecordRespVO();
        slitting.setReportDate(businessDate);
        slitting.setRecordTime(time);
        HcCutRoundProductionRecordRespVO cut = new HcCutRoundProductionRecordRespVO();
        cut.setReportDate(businessDate);
        cut.setRecordTime(time);
        Object[] exports = {
            ReflectionTestUtils.invokeMethod(new HcAdhesiveProductionRecordController(), "toAdhesive1Excel", adhesive),
            ReflectionTestUtils.invokeMethod(new HcAdhesiveProductionRecordController(), "toAdhesive2Excel", adhesive),
            ReflectionTestUtils.invokeMethod(new HcSlittingPressProductionRecordController(), "toExcel", slitting),
            ReflectionTestUtils.invokeMethod(new HcCutRoundProductionRecordController(), "toExcel", cut)
        };
        for (Object exported : exports) {
            assertEquals(expected, ReflectionTestUtils.getField(exported, "reportDate"));
        }
        assertEquals(businessDate, adhesive.getReportDate());
        assertEquals(businessDate, slitting.getReportDate());
        assertEquals(businessDate, cut.getReportDate());
    }

    private void initTable(Class<?> type) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "daily-record-test");
        assistant.setCurrentNamespace(type.getName());
        TableInfoHelper.initTableInfo(assistant, type);
    }
}
