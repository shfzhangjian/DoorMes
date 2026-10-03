// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.andon.AndonRecordService.java
package cn.iocoder.yudao.module.mes.service.andon;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.andon.AndonRecordDO;
import cn.iocoder.yudao.module.mes.controller.admin.andon.vo.AndonRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.andon.vo.AndonRecordPageReqVO;

import jakarta.validation.Valid;

public interface AndonRecordService {
    Long createAndonRecord(@Valid AndonRecordSaveReqVO createReqVO);
    void updateAndonRecord(@Valid AndonRecordSaveReqVO updateReqVO);
    void deleteAndonRecord(Long id);
    AndonRecordDO getAndonRecord(Long id);
    PageResult<AndonRecordDO> getAndonRecordPage(AndonRecordPageReqVO pageReqVO);
}
