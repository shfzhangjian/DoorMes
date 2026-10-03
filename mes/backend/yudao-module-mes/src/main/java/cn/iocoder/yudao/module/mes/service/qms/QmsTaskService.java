// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.qms.QmsTaskServiceImpl.java
package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsTaskDO;

public interface QmsTaskService {
    Long createQmsTask(QmsTaskSaveReqVO createReqVO);
    void updateQmsTask(QmsTaskSaveReqVO updateReqVO);
    PageResult<QmsTaskDO> getQmsTaskPage(QmsTaskPageReqVO pageReqVO);
}
