package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_supplier_review_reply")
@KeySequence("mes_srm_supplier_review_reply_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierReviewReplyDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long yearPlanId;
    private Long lineId;
    private Long monthPlanId;
    private Integer planYear;
    private Integer planMonth;
    private LocalDateTime replyTime;
    private LocalDate reviewDate;
    private Long recorderUserId;
    private String recorderUserName;
    private String reviewResult;
    private String remark;
    private Long tenantId;

}
