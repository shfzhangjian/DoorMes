package cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 量检具状态调整处理记录。 */
@TableName("mes_qms_measure_tool_status_record")
@KeySequence("mes_qms_measure_tool_status_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsMeasureToolStatusRecordDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ledgerId;
    private String previousStatus;
    private String status;
    private String handler;
    private LocalDateTime handleTime;
    private String oaProcessNo;
    private String handleRemark;
    /** 多个文件 URL 以英文逗号分隔。 */
    private String attachments;
    private Long tenantId;

}
