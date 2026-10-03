// 完整路径: cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteSaveReqVO
package cn.iocoder.yudao.module.mes.controller.admin.route.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 工艺路线新增/修改 Request VO")
@Data
public class RouteSaveReqVO {

    @Schema(description = "主键ID", example = "4001")
    private Long id;

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "工艺路线编号不能为空")
    private String code;

    @Schema(description = "工艺路线名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "工艺路线名称不能为空")
    private String name;

    @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    // --- 冗余字段 (前端回填后传给后端) ---
    private String productCode;
    private String productName;
    private String productSpec;
    private String productUnit;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "是否默认路线")
    @NotNull(message = "是否默认路线不能为空")
    private Boolean active;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "工序明细列表")
    private List<RouteProcess> processes;

    /**
     * 子表 VO (RouteProcess) - 工序节点
     */
    @Data
    public static class RouteProcess {
        private Long id;

        @NotNull(message = "工序ID不能为空")
        private Long processId;

        // 冗余字段
        private String processCode;
        private String processName;

        @NotNull(message = "顺序不能为空")
        private Integer seqNo;

        private Long nextProcessId;

        private Boolean keyNode;
        private BigDecimal standardTime;
        private String timeUnit;
        private String remark;

        // ================= [4M1E 新增与修改] =================

        // 1. Environment (环/测) - 已有, 保持不变
        @Schema(description = "关键工艺参数 (Environment)")
        private List<RouteProcessParam> params;

        // 2. Material (料) - [新增]
        @Schema(description = "工序投料清单 (Material)")
        private List<RouteProcessInput> inputs;

        // 3. Man (人) - [新增]
        @Schema(description = "岗位定额 (Man)")
        private List<RouteProcessPost> posts;

        // 4. Method (法) - [新增]
        @Schema(description = "作业指导书 (Method)")
        private List<RouteProcessSop> sops;
    }

    /**
     * 孙表 VO 1: 参数 (Environment) - 保持原有结构
     */
    @Data
    public static class RouteProcessParam {
        private Long id;
        private String paramCode;
        private String paramName;
        // V3.1 新增字段支持
        @Schema(description = "参数类型(numeric, text, boolean)")
        private String paramType;
        private String standardValue;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private String unit;
        private Boolean critical;
        private String remark;
    }

    /**
     * 孙表 VO 2: 投料 (Material) - [新增]
     */
    @Data
    public static class RouteProcessInput {
        private Long id;
        @NotNull(message = "物料ID不能为空")
        private Long materialId;
        private String materialCode;
        private String materialName;
        @NotNull(message = "标准用量不能为空")
        private BigDecimal standardQty;
        private String unit;
        private BigDecimal lossRate; // 损耗率
        private String remark;
    }

    /**
     * 孙表 VO 3: 岗位 (Man) - [新增]
     */
    @Data
    public static class RouteProcessPost {
        private Long id;
        @NotEmpty(message = "岗位编码不能为空")
        private String postCode;
        private String postName;
        private String skillLevel; // 技能等级 L1-L5
        private BigDecimal stdManHour; // 标准工时
        private Integer minPerson; // 最少人数
    }

    /**
     * 孙表 VO 4: SOP (Method) - [新增]
     */
    @Data
    public static class RouteProcessSop {
        private Long id;
        @NotEmpty(message = "文件编号不能为空")
        private String docCode;
        private String docName;
        private String docUrl;
        private String version;
        private Boolean critical; // 是否强制阅读
    }
}
