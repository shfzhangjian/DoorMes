package cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap;

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

@TableName("mes_md_finished_glue_board_map")
@KeySequence("mes_md_finished_glue_board_map_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFinishedGlueBoardMapDO extends BaseDO {

    @TableId
    private Long id;

    private Long productModelId;
    private String productModelCode;
    private String productModelName;
    private String productSpec;
    private String sizeSpec;
    private String sizeName;
    private String adhesive1Summary;
    private String adhesive2Summary;
    private String status;
    private String remark;
    private Long tenantId;

}
