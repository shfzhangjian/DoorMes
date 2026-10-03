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

@TableName("mes_md_finished_glue_board_map_item")
@KeySequence("mes_md_finished_glue_board_map_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFinishedGlueBoardMapItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long mapId;
    private String productModelCode;
    private String glueProcess;
    private String glueProcessName;
    private Long glueBoardMaterialId;
    private String glueBoardMaterialCode;
    private String glueBoardMaterialName;
    private String glueBoardModel;
    private String glueBoardSpec;
    private Boolean preferredFlag;
    private Integer sort;
    private String remark;
    private Long tenantId;

}
