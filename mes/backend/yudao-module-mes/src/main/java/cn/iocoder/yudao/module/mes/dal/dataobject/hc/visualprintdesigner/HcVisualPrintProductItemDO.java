package cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner;

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

@TableName("mes_hc_visual_print_product_item")
@KeySequence("mes_hc_visual_print_product_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcVisualPrintProductItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long customerInfoId;
    private Integer sourceRow;
    private String productType;
    private String sizeMm;
    private String padBackLabelImageId;
    private String padBackLabelImageFile;
    private String cleanBagLabelImageId;
    private String cleanBagLabelImageFile;
    private String boxFrontLabelImageId;
    private String boxFrontLabelImageFile;
    private String customerSideLabelImageId;
    private String customerSideLabelImageFile;
    private Integer status;
    private String importBatchNo;
    private String rawJson;
    private Long tenantId;

}
