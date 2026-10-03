// 完整路径: cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteRespVO
package cn.iocoder.yudao.module.mes.controller.admin.route.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 工艺路线 Response VO")
@Data
@ExcelIgnoreUnannotated
public class RouteRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @ExcelProperty("编号")
    private String code;
    @ExcelProperty("名称")
    private String name;

    private Long productId;
    @ExcelProperty("产品名称")
    private String productName;
    private String productCode;
    private String productSpec;
    private String productUnit;

    @ExcelProperty("版本")
    private String version;
    @ExcelProperty("是否默认")
    private Boolean active;
    @ExcelProperty("状态")
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    @Schema(description = "工序明细列表")
    private List<RouteProcess> processes;

    @Data
    public static class RouteProcess {
        private Long id;
        private Long processId;
        private String processCode;
        private String processName;
        private Integer seqNo;
        private Long nextProcessId;
        private Boolean keyNode;
        private BigDecimal standardTime;
        private String timeUnit;
        private String remark;

        // ================= [4M1E 新增] =================
        @Schema(description = "关键工艺参数 (Environment)")
        private List<RouteProcessParam> params;

        @Schema(description = "工序投料清单 (Material)")
        private List<RouteProcessInput> inputs;

        @Schema(description = "岗位定额 (Man)")
        private List<RouteProcessPost> posts;

        @Schema(description = "作业指导书 (Method)")
        private List<RouteProcessSop> sops;
    }

    // --- 孙表 VO 定义 ---

    @Data
    public static class RouteProcessParam {
        private Long id;
        private String paramCode;
        private String paramName;
        private String paramType;
        private String standardValue;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private String unit;
        private Boolean critical;
        private String remark;
    }

    @Data
    public static class RouteProcessInput {
        private Long id;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal standardQty;
        private String unit;
        private BigDecimal lossRate;
        private String remark;
    }

    @Data
    public static class RouteProcessPost {
        private Long id;
        private String postCode;
        private String postName;
        private String skillLevel;
        private BigDecimal stdManHour;
        private Integer minPerson;
    }

    @Data
    public static class RouteProcessSop {
        private Long id;
        private String docCode;
        private String docName;
        private String docUrl;
        private String version;
        private Boolean critical;
    }
}
