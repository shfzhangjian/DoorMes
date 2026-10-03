package cn.iocoder.yudao.module.mes.dal.dataobject.hc.visualprintdesigner;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_hc_visual_print_design")
@KeySequence("mes_hc_visual_print_design_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcVisualPrintDesignDO extends BaseDO {

    @TableId
    private Long id;

    private Long customerInfoId;
    private Long productItemId;
    private String labelKind;
    private String labelName;
    private BigDecimal widthMm;
    private BigDecimal heightMm;
    private Integer dpi;
    private String imageId;
    private String imageFile;
    private Integer imageWidthPx;
    private Integer imageHeightPx;
    private String btwTemplateRootDir;
    private String btwCallFile;
    private String designJson;
    private Integer status;
    private String remark;
    private Long tenantId;

}
