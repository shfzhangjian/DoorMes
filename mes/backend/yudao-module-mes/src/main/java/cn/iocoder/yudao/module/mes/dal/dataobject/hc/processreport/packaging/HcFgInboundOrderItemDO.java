package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_inv_fg_inbound_order_item")
@KeySequence("mes_inv_fg_inbound_order_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFgInboundOrderItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long inboundOrderId;
    private String inboundNo;
    private Long outerBoxId;
    private String outerBoxNo;
    private Integer pieceQty;
    private String materialCode;
    private String modelCode;
    private String batchNo;
    private String qualityStatus;
    private Long tenantId;
}
