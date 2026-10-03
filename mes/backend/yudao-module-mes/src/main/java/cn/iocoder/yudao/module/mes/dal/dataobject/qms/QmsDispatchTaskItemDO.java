package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 质量任务选中的检验项目快照。
 */
@TableName("mes_qms_dispatch_task_item")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsDispatchTaskItemDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer roundNo;
    private Long sourceItemId;
    private Long executionItemId;
    private Long standardItemId;
    private String pieceNo;
    private String inspectionItem;
    private String standardDesc;
    private String itemUnit;
    private String itemType;
    private String inspectionMethod;
    private String testTool;
    private Integer sampleSize;
    private BigDecimal measuredValue;
    private String qualitativeValue;
    private String result;
    private Integer sort;
}
