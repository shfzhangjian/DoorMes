package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_document")
@KeySequence("mes_srm_document_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmDocumentDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizType;
    private String docNo;
    private String title;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String materialName;
    private String status;
    private Long applicantId;
    private String applicantName;
    private String applyDept;
    private LocalDateTime applyTime;
    private LocalDate dueDate;
    private BigDecimal totalScore;
    private String evalGrade;
    private String bizCategory;
    private String bizLevel;
    private String periodType;
    private Integer evalYear;
    private Integer evalQuarter;
    private String payloadJson;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
