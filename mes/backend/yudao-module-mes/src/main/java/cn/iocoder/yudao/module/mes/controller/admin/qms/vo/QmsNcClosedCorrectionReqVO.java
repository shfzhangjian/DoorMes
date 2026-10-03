package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.Data;

@Data
public class QmsNcClosedCorrectionReqVO {
    @NotNull private Long id;
    @NotBlank @Size(max = 500) private String reason;
    @NotEmpty @Size(max = 2000) private List<@NotNull @Valid Change> changes;
    private String previewToken;

    @Data
    public static class Change {
        @NotNull private Long scopeId;
        @NotBlank private String originalDisposition;
        @NotBlank @Pattern(regexp = "PICK|SCRAP") private String dispositionType;
    }
}
