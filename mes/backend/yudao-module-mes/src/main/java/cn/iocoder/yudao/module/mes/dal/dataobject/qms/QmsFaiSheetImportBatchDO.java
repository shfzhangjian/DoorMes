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

@TableName("mes_qms_fai_sheet_import_batch")
@KeySequence("mes_qms_fai_sheet_import_batch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFaiSheetImportBatchDO extends BaseDO {

    @TableId
    private Long id;

    private Long faiId;

    private Long targetFaiId;

    private String targetFaiNo;

    private String importBatchNo;

    private String fileName;

    private Long sheetTemplateId;

    private String templateCode;

    private String templateVersion;

    private String templateVersionHash;

    private String status;

    private String importUsage;

    private String validateSummary;

    private Boolean allowOverwrite;

    private String reviewStatus;

    private Integer successCount;

    private Integer failureCount;

    private Integer warningCount;

    private String errorSummary;

    private Long importerId;

    private String importerName;

    private LocalDateTime importTime;

    private Long tenantId;
}
