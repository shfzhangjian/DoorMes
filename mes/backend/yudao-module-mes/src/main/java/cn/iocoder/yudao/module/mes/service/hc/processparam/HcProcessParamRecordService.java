package cn.iocoder.yudao.module.mes.service.hc.processparam;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processparam.HcProcessParamRecordDO;
import cn.iocoder.yudao.module.mes.service.hc.processparam.dto.HcProcessParamRecordUpsertReq;
import java.util.Collection;
import java.util.List;

public interface HcProcessParamRecordService {

    String PARAM_CODE_THICKNESS = "THICKNESS";
    String PARAM_NAME_THICKNESS = "厚度";

    Long upsertProcessParam(HcProcessParamRecordUpsertReq req);

    List<HcProcessParamRecordDO> listByBatchNosAndProcessCodes(Collection<String> batchNos,
                                                               Collection<String> processCodes,
                                                               String paramCode);

}
