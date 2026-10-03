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

@TableName("mes_srm_scar")
@KeySequence("mes_srm_scar_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmScarDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 台账编号，格式 SCAR-yyyyMMddXXX */
    private String scarNo;
    /** 供应商ID（可空，支持未入库供应商） */
    private Long supplierId;
    /** 供应商代码 */
    private String supplierCode;
    /** 供应商名称 */
    private String supplierName;
    /** 物料ID（可空） */
    private Long materialId;
    /** 物料代码 */
    private String materialCode;
    /** 物料名称 */
    private String materialName;
    /** 物料型号 */
    private String materialModel;
    /** 物料批次 */
    private String batchNo;
    /** 数量 */
    private BigDecimal quantity;
    /** 异常发生日期 */
    private LocalDate issueDate;
    /** 异常描述 */
    private String issueDesc;
    /** 异常回复日期 */
    private LocalDate replyDate;
    /** 异常回复说明 */
    private String replyDesc;
    /** 状态：WAIT_SUPPLIER待供方回复、CLOSED已整改关闭 */
    private String status;
    /** 登记人ID */
    private Long applicantId;
    /** 登记人姓名快照 */
    private String applicantName;
    /** 登记时间 */
    private LocalDateTime applyTime;
    /** 备注 */
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}