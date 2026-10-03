package cn.iocoder.yudao.module.mes.controller.admin.hc.visualprintdesigner.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class HcVisualPrintDesignAttachReqVO {

    @NotNull(message = "客户打印信息ID不能为空")
    private Long customerInfoId;
    @NotNull(message = "设计稿ID不能为空")
    private Long designId;
    @NotBlank(message = "标签类型不能为空")
    private String labelKind;
    @NotEmpty(message = "请选择需要挂接的产品型号尺寸")
    private List<Long> productItemIds;

}
