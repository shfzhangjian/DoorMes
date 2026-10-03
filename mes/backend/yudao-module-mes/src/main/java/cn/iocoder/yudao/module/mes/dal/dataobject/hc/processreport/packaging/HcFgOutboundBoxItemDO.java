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

@TableName("mes_inv_fg_outbound_box_item")
@KeySequence("mes_inv_fg_outbound_box_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFgOutboundBoxItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long outboundBoxId;
    private String outboundBoxNo;
    private Long outboundOrderId;
    private String outboundNo;
    private Long finishedStockId;
    private String inboundNo;
    private String inboundBoxNo;
    private String inboundInnerUnitNo;
    private String sliceBatchNo;
    private String materialCode;
    private String modelCode;
    private String batchNo;
    private String qualityStatus;
    private String scanUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime scanTime;
    private Long tenantId;
}
