package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardSaveReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QmsQualityStandardAttachmentPolicyTest {

    private final QmsQualityStandardServiceImpl service = new QmsQualityStandardServiceImpl();

    @Test
    void shouldForceAttachmentEnabledForIqcAndFaiStandardItems() {
        QmsQualityStandardSaveReqVO.StandardItem iqcItem = buildQualitativeItem();
        QmsQualityStandardSaveReqVO.StandardItem faiItem = buildQualitativeItem();

        ReflectionTestUtils.invokeMethod(service, "validateEntryRules", List.of(iqcItem), "IQC");
        ReflectionTestUtils.invokeMethod(service, "validateEntryRules", List.of(faiItem), "FAI");

        assertThat(iqcItem.getAttachmentEnabled()).isTrue();
        assertThat(faiItem.getAttachmentEnabled()).isTrue();
    }

    @Test
    void shouldNotChangeAttachmentPolicyForOtherStandardTypes() {
        QmsQualityStandardSaveReqVO.StandardItem item = buildQualitativeItem();

        ReflectionTestUtils.invokeMethod(service, "validateEntryRules", List.of(item), "IPQC");

        assertThat(item.getAttachmentEnabled()).isFalse();
    }

    private QmsQualityStandardSaveReqVO.StandardItem buildQualitativeItem() {
        QmsQualityStandardSaveReqVO.StandardItem item = new QmsQualityStandardSaveReqVO.StandardItem();
        item.setInspectionItem("外观");
        item.setItemType("QUALITATIVE");
        item.setStandardDesc("外观应符合要求");
        item.setAttachmentEnabled(false);
        return item;
    }
}
