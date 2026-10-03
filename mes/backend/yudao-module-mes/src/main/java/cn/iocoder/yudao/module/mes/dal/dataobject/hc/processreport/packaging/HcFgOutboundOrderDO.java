package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

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

@TableName("mes_inv_fg_outbound_order")
@KeySequence("mes_inv_fg_outbound_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFgOutboundOrderDO extends BaseDO {

    @TableId
    private Long id;

    private String outboundNo;
    private Long sourceSaleOrderId;
    private Long sourceNoticeId;
    private String shippingOrderNo;
    private String shippingNoticeNo;
    private String customerCode;
    private String customerName;
    private String erpOrderNo;
    private String orderNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String productType;
    private String productSize;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime shippingTime;
    private Integer shipQty;
    private Integer boxCount;
    private Integer pieceCount;
    private String outboundStatus;
    private String recorderName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;
    private String confirmerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmerTime;
    private String remark;
    private Long tenantId;
}
