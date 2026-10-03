package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_fqc_item_photo")
@KeySequence("mes_qms_fqc_item_photo_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFqcItemPhotoDO extends BaseDO {

    @TableId
    private Long id;

    private Long fqcId;
    private String fqcNo;
    private Long submissionDetailId;
    private Long fqcItemId;
    private Long sampleId;
    private String productionBatchNo;
    private String inspectionItem;
    private String photoUrl;
    /** RESULT/DEFECT/RECHECK。 */
    private String photoScene;
    private Long defectCodeId;
    private String defectCode;
    private String remark;
    private Integer sort;
    private Long capturedById;
    private String capturedByName;
    private LocalDateTime capturedTime;
    private Long tenantId;
}
