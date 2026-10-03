package cn.iocoder.yudao.module.mes.service.qms;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QmsIqcInspectionApplyAttachmentTest {

    @Test
    void shouldNormalizeSupportedAttachments() {
        List<String> normalized = QmsIqcServiceImpl.normalizeInspectionApplyAttachmentUrls(List.of(
                " https://files.example.com/送检照片.JPG?token=1 ",
                "https://files.example.com/来料报告.pdf",
                "https://files.example.com/来料报告.pdf",
                " "));

        assertEquals(List.of(
                "https://files.example.com/送检照片.JPG?token=1",
                "https://files.example.com/来料报告.pdf"), normalized);
    }

    @Test
    void shouldRejectUnsupportedAttachmentType() {
        assertThrows(RuntimeException.class,
                () -> QmsIqcServiceImpl.normalizeInspectionApplyAttachmentUrls(
                        List.of("https://files.example.com/install.exe")));
    }

    @Test
    void shouldRejectMoreThanTenAttachments() {
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            urls.add("/infra/file/1/get/iqc/attachment-" + i + ".pdf");
        }

        assertThrows(RuntimeException.class,
                () -> QmsIqcServiceImpl.normalizeInspectionApplyAttachmentUrls(urls));
    }

    @Test
    void shouldRejectUnsafeAttachmentScheme() {
        assertThrows(RuntimeException.class,
                () -> QmsIqcServiceImpl.normalizeInspectionApplyAttachmentUrls(
                        List.of("javascript:alert(1).pdf")));
    }
}
