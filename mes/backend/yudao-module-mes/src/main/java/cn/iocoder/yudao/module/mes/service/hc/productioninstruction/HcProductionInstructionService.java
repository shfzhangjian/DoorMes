package cn.iocoder.yudao.module.mes.service.hc.productioninstruction;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionMessagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionChangeoverPieceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionChangeoverStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionOperationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionRevokeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import java.util.List;

public interface HcProductionInstructionService {

    Long issueProductionInstruction(HcProductionInstructionSaveReqVO reqVO);

    void updateProductionInstruction(HcProductionInstructionSaveReqVO reqVO);

    void deleteProductionInstruction(Long id);

    HcProductionInstructionDO getProductionInstruction(Long id);

    List<HcProductionInstructionDO> getProductionInstructionList(HcProductionInstructionPageReqVO reqVO);

    PageResult<HcProductionInstructionDO> getProductionInstructionPage(HcProductionInstructionPageReqVO pageReqVO);

    List<HcProductionInstructionDO> getOperationInstructionList(HcProductionInstructionOperationReqVO reqVO);

    PageResult<HcProductionInstructionRespVO> getCurrentUserMessagePage(HcProductionInstructionMessagePageReqVO pageReqVO);

    Long getCurrentUserUnreadMessageCount(HcProductionInstructionMessagePageReqVO reqVO);

    void markCurrentUserMessageRead(Long id);

    void confirmProductionInstruction(Long id);

    void revokeProductionInstruction(HcProductionInstructionRevokeReqVO reqVO);

    HcProductionInstructionDO startChangeoverInstruction(HcProductionInstructionChangeoverStartReqVO reqVO);

    HcProductionInstructionDO recordChangeoverPiece(HcProductionInstructionChangeoverPieceReqVO reqVO);

}
