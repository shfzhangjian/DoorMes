package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import java.util.List;
import lombok.Data;

@Data
public class QmsNcClosedCorrectionRespVO {
    private Long id;
    private String ncNo;
    private String previewToken;
    private List<Piece> pieces;

    @Data
    public static class Piece {
        private Long scopeId;
        private String pieceNo;
        private String originalDisposition;
        private String dispositionType;
        private boolean editable;
        private String blockedReason;
    }
}
