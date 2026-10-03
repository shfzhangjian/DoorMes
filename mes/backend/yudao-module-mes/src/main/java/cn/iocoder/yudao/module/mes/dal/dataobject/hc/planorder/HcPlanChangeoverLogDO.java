package cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_plan_changeover_log")
@KeySequence("mes_sfc_plan_changeover_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPlanChangeoverLogDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String beforeMaterialCode;
    private String afterMaterialCode;
    private String beforeModelCode;
    private String afterModelCode;
    private String beforeGlueBoardModel;
    private String afterGlueBoardModel;
    private Boolean changeoverFlag;
    private Long changeoverUserId;
    private String changeoverUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime changeoverTime;
    private String remark;
    private String extraJson;
    private Long tenantId;
}
