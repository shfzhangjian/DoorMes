package cn.iocoder.yudao.module.mes.dal.dataobject.hc.sliceadjust;

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

@TableName("mes_sfc_slice_adjust_record")
@KeySequence("mes_sfc_slice_adjust_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcSliceAdjustRecordDO extends BaseDO {

    @TableId
    private Long id;

    private String adjustNo;
    private String adjustType;
    private String segmentBatchNo;
    private String leftSliceNo;
    private String rightSliceNo;
    private String leftLastProcessCode;
    private String leftLastProcessName;
    private String rightLastProcessCode;
    private String rightLastProcessName;
    private String operatorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime adjustTime;
    private String adjustReason;
    private Integer affectedRows;
    private String affectedColumnsJson;
    private String beforeSnapshotJson;
    private String afterSnapshotJson;
    private Long tenantId;
}
