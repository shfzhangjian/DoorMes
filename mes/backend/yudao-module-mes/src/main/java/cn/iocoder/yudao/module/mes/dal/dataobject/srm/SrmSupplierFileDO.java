package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_supplier_file")
@KeySequence("mes_srm_supplier_file_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierFileDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String fileType;
    private String fileName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String providedProduct;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String productModel;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String inspectionAgency;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String reportCode;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate effectDate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate expiryDate;
    private String fileStatus;
    private Integer warningDays;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String standardCompliant;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer validityMonths;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String expiryRule;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String payloadJson;
    private String attachmentName;
    private String attachmentUrl;
    private String sourceBizType;
    private Long sourceBizId;
    private String remark;

    private Long tenantId;

}
