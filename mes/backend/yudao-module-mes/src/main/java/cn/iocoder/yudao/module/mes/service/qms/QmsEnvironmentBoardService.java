package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentBoardReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentBoardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordCorrectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsEnvironmentStandardSaveReqVO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface QmsEnvironmentBoardService {

    QmsEnvironmentBoardRespVO getBoard(QmsEnvironmentBoardReqVO reqVO);

    QmsEnvironmentBoardRespVO.Standard saveStandard(QmsEnvironmentStandardSaveReqVO reqVO);

    QmsEnvironmentBoardRespVO.Record saveRecord(QmsEnvironmentRecordSaveReqVO reqVO);

    QmsEnvironmentBoardRespVO.Record correctRecord(QmsEnvironmentRecordCorrectReqVO reqVO);

    QmsEnvironmentBoardRespVO.Record confirmRecord(QmsEnvironmentRecordConfirmReqVO reqVO);

    List<QmsEnvironmentRecordExcelVO> buildExportList(QmsEnvironmentBoardReqVO reqVO);

    QmsEnvironmentImportRespVO importRecords(String workshopCode, String workshopName, String recordMonth,
                                             MultipartFile file, Long operatorId, String operatorUsername,
                                             String operatorName) throws IOException;
}
