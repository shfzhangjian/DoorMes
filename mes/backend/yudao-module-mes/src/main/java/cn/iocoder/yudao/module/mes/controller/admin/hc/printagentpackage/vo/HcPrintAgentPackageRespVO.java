package cn.iocoder.yudao.module.mes.controller.admin.hc.printagentpackage.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 打印程序版本 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcPrintAgentPackageRespVO {

    private Long id;

    @ExcelProperty("程序编码")
    private String packageCode;

    @ExcelProperty("版本号")
    private String versionNo;

    @ExcelProperty("附件名称")
    private String packageName;

    private String packageUrl;
    private String packageSha256;
    private Long packageSize;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("是否当前")
    private Boolean currentFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;

    private String releaseNote;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}
