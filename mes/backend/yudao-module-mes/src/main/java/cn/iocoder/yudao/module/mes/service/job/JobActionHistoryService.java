// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.job.JobActionHistoryService.java
package cn.iocoder.yudao.module.mes.service.job;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.job.JobActionHistoryDO;
import cn.iocoder.yudao.module.mes.controller.admin.job.vo.JobActionHistorySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.job.vo.JobActionHistoryPageReqVO;
import jakarta.validation.Valid;


/**
 * 动态SOP作业执行历史 Service 接口
 *
 * @author 资深后端架构智能体
 */
public interface JobActionHistoryService {

    /**
     * 记录作业执行历史 (核心执行引擎打卡接口)
     *
     * @param createReqVO 执行信息
     * @return 编号
     */
    Long createJobActionHistory(@Valid JobActionHistorySaveReqVO createReqVO);

    /**
     * 获得作业执行历史
     *
     * @param id 编号
     * @return 作业执行历史 DO
     */
    JobActionHistoryDO getJobActionHistory(Long id);

    /**
     * 获得作业执行历史分页
     *
     * @param pageReqVO 分页查询
     * @return 作业执行历史分页 DO
     */
    PageResult<JobActionHistoryDO> getJobActionHistoryPage(JobActionHistoryPageReqVO pageReqVO);

}
