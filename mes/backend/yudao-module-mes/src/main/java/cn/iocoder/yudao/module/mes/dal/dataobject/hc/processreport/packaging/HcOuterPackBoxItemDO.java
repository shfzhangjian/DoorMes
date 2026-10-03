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

@TableName("mes_sfc_outer_pack_box_item")
@KeySequence("mes_sfc_outer_pack_box_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcOuterPackBoxItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long outerBoxId;
    private String outerBoxNo;
    private Long innerUnitId;
    private String innerUnitNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Integer innerPackageSpec;
    private Integer pieceQty;
    private String sliceBatchListJson;
    private String scanUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime scanTime;
    private Long tenantId;
}
