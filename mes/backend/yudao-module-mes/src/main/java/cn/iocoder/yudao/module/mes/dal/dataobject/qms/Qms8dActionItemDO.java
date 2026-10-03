package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@TableName("mes_qms_8d_action_item")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Qms8dActionItemDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportId;
    private String reportNo;
    private String actionType;
    private String actionDesc;
    private String rootCauseCategory;
    private Long ownerUserId;
    private String ownerUserName;
    private Long ownerDeptId;
    private String ownerDeptName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planFinishDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualFinishDate;

    private String itemStatus;
    private String finishDesc;
    private String verificationResult;
    private Integer sort;
    private String remark;
    private Long tenantId;
}
