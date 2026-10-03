package cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 打印程序最新版本 Response VO")
@Data
public class HcPrintAgentPackageLatestRespVO {

    private String packageCode;
    private String versionNo;
    private String packageName;
    private String packageUrl;
    private String packageSha256;
    private Long packageSize;
    private String releaseNote;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;

}
