package cn.iocoder.yudao.module.mes.service.hc.processform;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordDO;

public interface HcProcessFormService {

    Long createRecord(HcProcessFormRecordSaveReqVO reqVO);

    void updateRecord(HcProcessFormRecordSaveReqVO reqVO);

    void deleteRecord(Long id);

    void submitRecord(Long id);

    void confirmRecord(Long id);

    void confirmRecordBySigner(Long id, Long confirmUserId);

    HcProcessFormRecordDO getRecord(Long id);

    PageResult<HcProcessFormRecordDO> getRecordPage(HcProcessFormRecordPageReqVO reqVO);

    HcProcessFormRecordDetailRespVO getRecordDetail(Long id);
}
