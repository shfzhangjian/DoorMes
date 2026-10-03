package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QmsFaiCompressionPrecisionTest {

    private static final List<String[]> COMPRESSION_INPUTS = List.of(
            new String[] {"0.880", "0.746", "0.877"},
            new String[] {"0.881", "0.747", "0.878"},
            new String[] {"0.885", "0.742", "0.881"},
            new String[] {"0.880", "0.744", "0.875"},
            new String[] {"0.880", "0.747", "0.874"},
            new String[] {"0.888", "0.740", "0.883"},
            new String[] {"0.885", "0.751", "0.880"},
            new String[] {"0.886", "0.753", "0.882"},
            new String[] {"0.881", "0.748", "0.878"},
            new String[] {"0.887", "0.755", "0.882"},
            new String[] {"0.888", "0.760", "0.883"},
            new String[] {"0.883", "0.762", "0.880"},
            new String[] {"0.885", "0.748", "0.880"},
            new String[] {"0.881", "0.747", "0.876"},
            new String[] {"0.878", "0.753", "0.874"});

    @Test
    void shouldCalculateFaiCompressionStatsBeforeDisplayRounding() {
        List<BigDecimal> compressionRates = calculateMetrics("compressionRate");
        assertEquals(new BigDecimal("15.227273"), compressionRates.get(0));
        assertEquals(new BigDecimal("15.133717"), calculateAverage(compressionRates));
        assertEquals(new BigDecimal("0.711692"), QmsFaiSampleResultWritebackService
                .calculateSampleStd(compressionRates, calculateAverage(compressionRates)));

        List<BigDecimal> elasticityRates = calculateMetrics("compressionElasticityRate");
        assertEquals(new BigDecimal("97.761194"), elasticityRates.get(0));
        assertEquals(new BigDecimal("96.760674"), calculateAverage(elasticityRates));
        assertEquals(new BigDecimal("0.708346"), QmsFaiSampleResultWritebackService
                .calculateSampleStd(elasticityRates, calculateAverage(elasticityRates)));
    }

    private List<BigDecimal> calculateMetrics(String judgmentMetric) {
        QmsFaiItemDO item = QmsFaiItemDO.builder()
                .itemType("QUANTITATIVE")
                .valueTemplate("COMPRESSION_CALC")
                .judgmentMetric(judgmentMetric)
                .templateParams("{\"dataRule\":{\"inputFields\":[],\"resultFields\":[],\"judgmentMetric\":\""
                        + judgmentMetric + "\"}}")
                .build();
        List<BigDecimal> results = new ArrayList<>();
        for (String[] values : COMPRESSION_INPUTS) {
            QmsFaiSaveReqVO.FaiSample sample = new QmsFaiSaveReqVO.FaiSample();
            sample.setRawValuesJson("{\"t1Mm\":" + values[0] + ",\"t2Mm\":" + values[1]
                    + ",\"t3Mm\":" + values[2] + "}");
            results.add(QmsFaiRuleCalculationSupport.calculateSampleMetric(item, sample, "COMPRESSION_CALC"));
        }
        return results;
    }

    private BigDecimal calculateAverage(List<BigDecimal> values) {
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }
}
