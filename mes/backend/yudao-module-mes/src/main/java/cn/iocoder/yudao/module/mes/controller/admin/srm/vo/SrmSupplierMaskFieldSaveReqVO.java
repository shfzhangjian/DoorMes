package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class SrmSupplierMaskFieldSaveReqVO {

    @Valid
    @NotEmpty(message = "脱敏字段配置不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @NotEmpty(message = "字段标识不能为空")
        private String fieldKey;

        @NotNull(message = "是否脱敏不能为空")
        private Boolean maskEnabled;

    }

}
