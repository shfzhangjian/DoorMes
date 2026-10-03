package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QmsFaiAttachmentTest {

    private final QmsFaiServiceImpl service = new QmsFaiServiceImpl();

    @Test
    void shouldNormalizeAttachmentUrlsWhenEnabled() {
        QmsFaiItemDO item = QmsFaiItemDO.builder()
                .inspectionItem("外观")
                .attachmentEnabled(true)
                .build();

        ReflectionTestUtils.invokeMethod(service, "applyAttachmentUrls", item,
                List.of(" https://files.example.com/a.png ", "", "https://files.example.com/a.png",
                        "https://files.example.com/report.pdf"));

        assertThat(item.getAttachmentUrls()).containsExactly(
                "https://files.example.com/a.png",
                "https://files.example.com/report.pdf");
    }

    @Test
    void shouldRejectAttachmentWhenItemDisabled() {
        QmsFaiItemDO item = QmsFaiItemDO.builder()
                .inspectionItem("外观")
                .attachmentEnabled(false)
                .build();

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "applyAttachmentUrls", item,
                List.of("https://files.example.com/a.png")))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void shouldRejectMoreThanTenAttachments() {
        QmsFaiItemDO item = QmsFaiItemDO.builder()
                .inspectionItem("外观")
                .attachmentEnabled(true)
                .build();
        List<String> attachmentUrls = new ArrayList<>();
        for (int index = 0; index < 11; index++) {
            attachmentUrls.add("https://files.example.com/" + index + ".png");
        }

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "applyAttachmentUrls", item,
                attachmentUrls))
                .isInstanceOf(ServiceException.class);
    }
}
