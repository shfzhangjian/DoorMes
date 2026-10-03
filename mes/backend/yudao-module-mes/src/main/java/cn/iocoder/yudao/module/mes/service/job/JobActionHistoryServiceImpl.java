// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.job.JobActionHistoryServiceImpl.java
package cn.iocoder.yudao.module.mes.service.job;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX; // 🎯 修复: 引入 Yudao 增强查询包装器
import cn.iocoder.yudao.module.mes.controller.admin.job.vo.JobActionHistorySaveReqVO; // 🎯 修复: 指向 app 包
import cn.iocoder.yudao.module.mes.controller.admin.job.vo.JobActionHistoryPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.job.JobActionHistoryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.job.JobActionHistoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource; // 🎯 修复: 升级为 Jakarta
import java.time.LocalDateTime;

/**
 * 动态SOP作业执行历史 Service 实现类
 */
@Service
@Validated
public class JobActionHistoryServiceImpl implements JobActionHistoryService {

    @Resource // Jakarta EE
    private JobActionHistoryMapper jobActionHistoryMapper;

    @Override
    public Long createJobActionHistory(JobActionHistorySaveReqVO createReqVO) {
        // 🎯 修复: 因 SaveReqVO 已明确类型，BeanUtils.toBean 不再模棱两可
        JobActionHistoryDO historyDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, JobActionHistoryDO.class);

        if (historyDO.getExecuteTime() == null) {
            historyDO.setExecuteTime(LocalDateTime.now());
        }

        if (historyDO.getCompliant() == null) {
            historyDO.setCompliant(true);
        }

        jobActionHistoryMapper.insert(historyDO);
        return historyDO.getId();
    }

    @Override
    public JobActionHistoryDO getJobActionHistory(Long id) {
        return jobActionHistoryMapper.selectById(id);
    }

    @Override
    public PageResult<JobActionHistoryDO> getJobActionHistoryPage(JobActionHistoryPageReqVO pageReqVO) {
        // 🎯 修复: 替换为 LambdaQueryWrapperX 即可完美使用 eqIfPresent
        return jobActionHistoryMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<JobActionHistoryDO>()
                .eqIfPresent(JobActionHistoryDO::getSubOrderId, pageReqVO.getSubOrderId())
                .eqIfPresent(JobActionHistoryDO::getOperatorUser, pageReqVO.getOperatorUser())
                .eqIfPresent(JobActionHistoryDO::getCompliant, pageReqVO.getCompliant())
                .orderByDesc(JobActionHistoryDO::getId));
    }
}
