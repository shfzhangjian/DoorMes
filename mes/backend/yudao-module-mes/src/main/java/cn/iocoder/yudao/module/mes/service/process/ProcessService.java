// 完整路径: cn.iocoder.yudao.module.mes.service.process.ProcessService.java
package cn.iocoder.yudao.module.mes.service.process;

import java.util.*;
import jakarta.validation.*;
import cn.iocoder.yudao.module.mes.controller.admin.process.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO;
// [新增] 引入 DO
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessStationDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

/**
 * MES标准工序 Service 接口
 *
 * @author 演示管理员
 */
public interface ProcessService {

    /**
     * 创建MES标准工序
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createProcess(@Valid ProcessSaveReqVO createReqVO);

    /**
     * 更新MES标准工序
     *
     * @param updateReqVO 更新信息
     */
    void updateProcess(@Valid ProcessSaveReqVO updateReqVO);

    /**
     * 删除MES标准工序
     *
     * @param id 编号
     */
    void deleteProcess(Long id);

    /**
     * 批量删除MES标准工序
     *
     * @param ids 编号
     */
    void deleteProcessListByIds(List<Long> ids);

    /**
     * 获得MES标准工序
     *
     * @param id 编号
     * @return MES标准工序
     */
    ProcessDO getProcess(Long id);

    /**
     * 获得MES标准工序分页
     *
     * @param pageReqVO 分页查询
     * @return MES标准工序分页
     */
    PageResult<ProcessDO> getProcessPage(ProcessPageReqVO pageReqVO);

    // ================= [新增] 子表查询接口 =================

    /**
     * 获得工序关联的工位/设备列表
     *
     * @param processId 工序ID
     * @return 工位关联列表
     */
    List<ProcessStationDO> getProcessStationList(Long processId);

}
