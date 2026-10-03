package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class QmsQualityStandardContentHashServiceTest {

    private final QmsQualityStandardContentHashService service =
            new QmsQualityStandardContentHashService();

    @Test
    void shouldDetectIqcStandardRowAddDeleteAndModify() {
        QmsQualityStandardItemDO first = iqcStandardItem("外观", 1, "表面无划伤");
        QmsQualityStandardItemDO second = iqcStandardItem("有效期", 2, "距当前日期不超过5天");
        String original = service.hashIqcStandardItems(List.of(first, second));

        assertNotEquals(original, service.hashIqcStandardItems(List.of(first)));
        assertNotEquals(original, service.hashIqcStandardItems(List.of(
                iqcStandardItem("外观", 1, "允许轻微划伤"), second)));
        assertNotEquals(original, service.hashIqcStandardItems(List.of(
                first, second, iqcStandardItem("包装", 3, "包装完整"))));
    }

    @Test
    void shouldGenerateSameHashForIqcStandardAndItsSnapshot() {
        QmsQualityStandardItemDO standard = iqcStandardItem("有效期", 1, "距当前日期不超过5天");
        standard.setSort(null);
        standard.setItemType("DATE");
        standard.setExpiryDays(5);
        standard.setAttachmentEnabled(true);
        standard.setSampleSize(1);
        QmsIqcItemDO snapshot = QmsIqcItemDO.builder()
                .sort(10)
                .inspectionItem(standard.getInspectionItem())
                .itemType(standard.getItemType())
                .expiryDays(standard.getExpiryDays())
                .attachmentEnabled(standard.getAttachmentEnabled())
                .standardDesc(standard.getStandardDesc())
                .sampleSize(standard.getSampleSize())
                .build();

        assertEquals(service.hashIqcStandardItems(List.of(standard)),
                service.hashIqcSnapshotItems(List.of(snapshot)));
    }

    @Test
    void shouldGenerateSameHashForFaiStandardAndItsSnapshot() {
        QmsQualityStandardItemDO standard = QmsQualityStandardItemDO.builder()
                .inspectionItem("厚度")
                .itemType("QUANTITATIVE")
                .attachmentEnabled(true)
                .standardDesc("1.00±0.05")
                .unit("mm")
                .sampleSize(3)
                .minValue(new BigDecimal("0.95"))
                .maxValue(new BigDecimal("1.05"))
                .valueTemplate("SINGLE_VALUE")
                .judgmentMetric("RESULT_VALUE")
                .build();
        QmsFaiItemDO snapshot = QmsFaiItemDO.builder()
                .sort(10)
                .inspectionItem(standard.getInspectionItem())
                .itemType(standard.getItemType())
                .attachmentEnabled(standard.getAttachmentEnabled())
                .standardDesc(standard.getStandardDesc())
                .unit(standard.getUnit())
                .sampleSize(standard.getSampleSize())
                .minValueLimit(standard.getMinValue())
                .maxValueLimit(standard.getMaxValue())
                .valueTemplate("SINGLE_VALUE")
                .judgmentMetric("RESULT_VALUE")
                .build();

        assertEquals(service.hashFaiStandardItems(List.of(standard)),
                service.hashFaiSnapshotItems(List.of(snapshot)));
    }

    private QmsQualityStandardItemDO iqcStandardItem(String name, int sort, String description) {
        return QmsQualityStandardItemDO.builder()
                .sort(sort)
                .inspectionItem(name)
                .itemType("QUALITATIVE")
                .standardDesc(description)
                .sampleSize(1)
                .build();
    }
}
