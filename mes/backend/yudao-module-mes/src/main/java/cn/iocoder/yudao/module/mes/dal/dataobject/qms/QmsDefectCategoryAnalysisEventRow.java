package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QmsDefectCategoryAnalysisEventRow {

    private String eventSource;
    private String processCode;
    private String processName;
    private String inspectionType;
    private String sourceTable;
    private Long sourceId;
    private Long inspectionId;
    private String inspectionNo;
    private String planNo;
    private String motherRollBatchNo;
    private String segmentBatchNo;
    private String scanConfirmPieceNo;
    private String processPieceNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String defectCode;
    private String defectName;
    private String defectLevel;
    private String inspectionCategory;
    private String inspectorName;
    private String checkResult;
    private String remark;
    private String visualResultJson;
    private LocalDateTime scanConfirmTime;
    private LocalDateTime inspectionTime;
    private LocalDateTime eventTime;
    private Integer quantity;
}
