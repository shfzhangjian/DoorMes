package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsStageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsPieceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsInspectionRespVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;

class QmsMotherRollProcessLossTest {
    private final QmsMotherRollGoodStatisticsServiceImpl service = new QmsMotherRollGoodStatisticsServiceImpl();

    private QmsMotherRollGoodStatisticsStageRespVO stage(String code) {
        var stage = new QmsMotherRollGoodStatisticsStageRespVO();
        stage.setStageCode(code);
        var piece = new QmsMotherRollGoodStatisticsPieceRespVO();
        piece.setPieceNo("W26J032AQ081A");
        piece.setDefectFlag(true);
        piece.setSourceSlittingOkFlag(true);
        stage.setPieceDetails(List.of(piece));
        stage.setDoneQty(BigDecimal.TEN);
        return stage;
    }

    private QmsMotherRollGoodStatisticsInspectionRespVO sample(String pieceNo, String status) {
        var item = new QmsMotherRollGoodStatisticsInspectionRespVO();
        item.setProductBatchNo(pieceNo);
        item.setInspectionNo("FAI-20260911-022");
        item.setSourceType("FAI");
        item.setFirstInspectionSampleFlag(true);
        item.setSourceSlittingOkFlag(true);
        item.setStatus(status);
        item.setJudgment("OK");
        return item;
    }

    private BigDecimal loss(QmsMotherRollGoodStatisticsStageRespVO stage,
                            QmsMotherRollGoodStatisticsInspectionRespVO... inspections) {
        return ReflectionTestUtils.invokeMethod(service, "processLossQty", stage.getStageCode(), stage, List.of(inspections));
    }

    @Test
    void counts081NgAnd068OkSampleWithoutChangingOutput() {
        var stage = stage("PRESS_SLOT");
        assertThat(loss(stage, sample("W26J032AQ068A", "COMPLETED"))).isEqualByComparingTo("2");
        assertThat(stage.getPieceDetails()).hasSize(1);
        assertThat(stage.getDoneQty()).isEqualByComparingTo("10");
    }

    @Test
    void countsSampleEvenWhenNoNormalPiecesExist() {
        var stage = stage("PRESS_SLOT");
        stage.setPieceDetails(List.of());
        assertThat(loss(stage, sample("W26J032AQ068A", "COMPLETED"))).isEqualByComparingTo("1");
    }

    @Test
    void deduplicatesRepeatedSampleAndNgOverlap() {
        assertThat(loss(stage("PRESS_SLOT"), sample("W26J032AQ068A", "COMPLETED"),
                sample(" w26j032aq068a ", "COMPLETED"), sample("W26J032AQ081A", "COMPLETED")))
                .isEqualByComparingTo("2");
    }

    @Test
    void doesNotUseSegmentOrInspectionNumberAsPiece() {
        assertThat(loss(stage("PRESS_SLOT"), sample("W26J032AQ", "COMPLETED"),
                sample(null, "COMPLETED"))).isEqualByComparingTo("1");
    }

    @Test
    void doesNotAddUncompletedOrCanceledUnmatchedSamples() {
        assertThat(loss(stage("PRESS_SLOT"), sample("W26J032AQ068A", "CANCELED"),
                sample("W26J032AQ069A", "PENDING"))).isEqualByComparingTo("1");
    }

    @Test
    void doesNotAddUnmatchedProcessChecks() {
        var check = sample("W26J032AQ088A", "COMPLETED");
        check.setFirstInspectionSampleFlag(false);
        assertThat(loss(stage("PRESS_SLOT"), check)).isEqualByComparingTo("1");
    }

    @Test
    void excludesSlittingNgFromPressNgAndMatchedInspection() {
        var stage = stage("PRESS_SLOT");
        stage.getPieceDetails().get(0).setSourceSlittingOkFlag(false);
        var inspection = sample("W26J032AQ081A", "COMPLETED");
        inspection.setSourceSlittingOkFlag(false);
        assertThat(loss(stage, inspection)).isEqualByComparingTo("0");
    }

    @Test
    void excludesSlittingNgFromFirstSampleFallback() {
        var sample = sample("W26J032AQ068A", "COMPLETED");
        sample.setSourceSlittingOkFlag(false);
        assertThat(loss(stage("PRESS_SLOT"), sample)).isEqualByComparingTo("1");
    }

    @Test
    void doesNotTreatMissingSlittingResultAsOk() {
        var stage = stage("PRESS_SLOT");
        stage.getPieceDetails().get(0).setSourceSlittingOkFlag(null);
        var sample = sample("W26J032AQ068A", "COMPLETED");
        sample.setSourceSlittingOkFlag(null);
        assertThat(loss(stage, sample)).isEqualByComparingTo("0");
    }

    @Test
    void leavesOtherProcessesUnchanged() {
        var stage = stage("ADHESIVE2");
        stage.getPieceDetails().get(0).setSourceSlittingOkFlag(false);
        assertThat(loss(stage, sample("W26J032AQ068A", "COMPLETED")))
                .isEqualByComparingTo("1");
    }
}
