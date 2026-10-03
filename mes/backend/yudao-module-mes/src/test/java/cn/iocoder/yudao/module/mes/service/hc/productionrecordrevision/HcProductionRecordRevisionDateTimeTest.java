package cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision;

import cn.hutool.core.bean.BeanUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionrecordrevision.vo.HcProductionRecordRevisionCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productionrecordrevision.HcProductionRecordRevisionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productionrecordrevision.HcProductionRecordRevisionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcProductionRecordRevisionDateTimeTest {

    private static final LocalDateTime REVISED_TIME = LocalDateTime.of(2026, 8, 19, 8, 30);
    private static final LocalDateTime ORIGINAL_TIME = LocalDateTime.of(2026, 8, 20, 20, 24, 52);

    @InjectMocks
    private HcProductionRecordRevisionServiceImpl service;
    @Mock
    private HcProductionRecordRevisionMapper revisionMapper;
    private ObjectMapper originalMapper;

    @BeforeEach
    void useApplicationTimestampDeserializer() {
        originalMapper = JsonUtils.getObjectMapper();
        JsonUtils.init(originalMapper.copy().registerModule(new SimpleModule("revision-test-timestamp")
                .addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE)));
    }

    @AfterEach
    void restoreMapper() {
        JsonUtils.init(originalMapper);
    }

    @Test
    void restoresWet800FromLatestRevisionWithoutChangingOtherFields() throws Exception {
        HcWetProductionRecordRespVO row = wetRow();
        row.setOutputMeter(new BigDecimal("85"));
        stubRevision("WET", "{\"recordDate\":\"2026-08-19\",\"recordTime\":\"2026-08-19 08:30:00\"}");

        service.applyRevisions("WET", List.of(row));

        assertEquals(REVISED_TIME, row.getRecordTime());
        assertEquals(LocalDate.of(2026, 8, 19), row.getRecordDate());
        assertEquals(new BigDecimal("85"), row.getOutputMeter());
        assertEquals("W26H19", row.getBatchNo());
        assertEquals("2026-08-19 08:30:00",
                JsonUtils.getObjectMapper().readTree(JsonUtils.toJsonString(row)).get("recordTime").asText());
        verify(revisionMapper, never()).insert(any(HcProductionRecordRevisionDO.class));
    }

    static Stream<Arguments> modules() {
        return Stream.of(
                Arguments.of("WET", HcWetProductionRecordRespVO.class),
                Arguments.of("FORMULA", HcFormulaProductionRecordRespVO.class),
                Arguments.of("ADHESIVE1", HcAdhesiveProductionRecordRespVO.class),
                Arguments.of("ADHESIVE2", HcAdhesiveProductionRecordRespVO.class),
                Arguments.of("CUT_ROUND", HcCutRoundProductionRecordRespVO.class),
                Arguments.of("SLITTING_PRESS", HcSlittingPressProductionRecordRespVO.class));
    }

    @ParameterizedTest
    @MethodSource("modules")
    void appliesStandardTimeToEverySharedModule(String module, Class<?> rowType) throws Exception {
        Object row = rowType.getDeclaredConstructor().newInstance();
        BeanUtil.setFieldValue(row, "id", 800L);
        stubRevision(module, "{\"recordTime\":\"2026-08-19 08:30:00\"}");
        service.applyRevisions(module, List.of(row));
        assertEquals(REVISED_TIME, BeanUtil.getFieldValue(row, "recordTime"));
    }

    static Stream<Object> historicalTimes() {
        long seconds = REVISED_TIME.atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond();
        return Stream.of("2026-08-19T08:30:00", "2026-08-19T00:30:00Z", seconds, seconds * 1000);
    }

    @ParameterizedTest
    @MethodSource("historicalTimes")
    void acceptsHistoricalIsoAndEpochValues(Object value) {
        stubRevision("WET", JsonUtils.toJsonString(Map.of("recordTime", value)));
        HcWetProductionRecordRespVO row = wetRow();
        service.applyRevisions("WET", List.of(row));
        assertEquals(REVISED_TIME, row.getRecordTime());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "bad-time", "08:30:00", "1970-01-01 08:00:00", "0"})
    void rejectsInvalidTimeBeforeSavingAndWhenReading(String value) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("recordTime", value);
        HcProductionRecordRevisionCreateReqVO request = new HcProductionRecordRevisionCreateReqVO();
        request.setModuleCode("WET");
        request.setRevisedData(fields);
        assertThrows(ServiceException.class, () -> service.createRevision(request));
        verifyNoInteractions(revisionMapper);

        stubRevision("WET", value == null ? "{\"recordTime\":null}" : JsonUtils.toJsonString(fields));
        HcWetProductionRecordRespVO row = wetRow();
        assertThrows(ServiceException.class, () -> service.applyRevisions("WET", List.of(row)));
        assertEquals(ORIGINAL_TIME, row.getRecordTime());
    }

    @Test
    void preservesOriginalTimeWhenOnlyNumericFieldChanges() {
        HcWetProductionRecordRespVO row = wetRow();
        stubRevision("WET", "{\"outputMeter\":86}");
        service.applyRevisions("WET", List.of(row));
        assertEquals(ORIGINAL_TIME, row.getRecordTime());
        assertEquals(new BigDecimal("86"), row.getOutputMeter());
    }

    private HcWetProductionRecordRespVO wetRow() {
        HcWetProductionRecordRespVO row = new HcWetProductionRecordRespVO();
        row.setId(800L);
        row.setBatchNo("W26H19");
        row.setRecordDate(ORIGINAL_TIME.toLocalDate());
        row.setRecordTime(ORIGINAL_TIME);
        return row;
    }

    private void stubRevision(String module, String json) {
        HcProductionRecordRevisionDO revision = new HcProductionRecordRevisionDO();
        revision.setRecordKey(module + ":800");
        revision.setRevisionNo(16);
        revision.setRevisedDataJson(json);
        when(revisionMapper.selectLatestList(eq(module), anyCollection())).thenReturn(List.of(revision));
    }
}
