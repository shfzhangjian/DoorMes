package cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 打印程序版本新增/修改 Request VO")
@Data
public class HcPrintAgentPackageSaveReqVO {

    private Long id;

    @NotBlank(message = "程序编码不能为空")
    private String packageCode;

    @NotBlank(message = "版本号不能为空")
    private String versionNo;

    @NotBlank(message = "附件名称不能为空")
    private String packageName;

    @NotBlank(message = "附件地址不能为空")
    private String packageUrl;

    private String packageSha256;
    private Long packageSize;
    private String status;
    private Boolean currentFlag;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime publishTime;

    private String releaseNote;
    private String remark;

}
