// 完整路径: cn.iocoder.yudao.module.mes.service.process.ProcessServiceImpl
package cn.iocoder.yudao.module.mes.service.process;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.module.mes.controller.admin.process.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO;
// [新增] 引入工位关联 DO
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessStationDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.mes.dal.mysql.process.ProcessMapper;
// [新增] 引入工位 Mapper
import cn.iocoder.yudao.module.mes.dal.mysql.process.ProcessStationMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES标准工序 Service 实现类
 *
 * @author 演示管理员
 */
@Service
@Validated
public class ProcessServiceImpl implements ProcessService {

    @Resource
    private ProcessMapper processMapper;
    // [新增] 注入子表 Mapper
    @Resource
    private ProcessStationMapper processStationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class) // [修改] 开启事务
    public Long createProcess(ProcessSaveReqVO createReqVO) {
        // 插入
        ProcessDO process = BeanUtils.toBean(createReqVO, ProcessDO.class);
        if (process.getStatus() == null) {
            process.setStatus(0);
        }
        if (process.getBindStation() == null) {
            process.setBindStation(false);
        }
        processMapper.insert(process);

        // [新增] 保存工位/设备矩阵
        createProcessStationList(process.getId(), createReqVO.getStations());

        // 返回
        return process.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // [修改] 开启事务
    public void updateProcess(ProcessSaveReqVO updateReqVO) {
        // 校验存在
        validateProcessExists(updateReqVO.getId());
        // 更新
        ProcessDO updateObj = BeanUtils.toBean(updateReqVO, ProcessDO.class);
        processMapper.updateById(updateObj);

        // [新增] 更新工位/设备矩阵
        updateProcessStationList(updateReqVO.getId(), updateReqVO.getStations());
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // [修改] 开启事务
    public void deleteProcess(Long id) {
        // 校验存在
        validateProcessExists(id);
        // 删除
        processMapper.deleteById(id);

        // [新增] 级联删除工位关联
        processStationMapper.deleteByProcessId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // [修改] 开启事务
    public void deleteProcessListByIds(List<Long> ids) {
        // 删除
        processMapper.deleteByIds(ids);

        // [新增] 级联删除工位关联 (循环删除或自定义批量删除 SQL)
        ids.forEach(id -> processStationMapper.deleteByProcessId(id));
    }


    private void validateProcessExists(Long id) {
        if (processMapper.selectById(id) == null) {
            throw exception(PROCESS_NOT_EXISTS);
        }
    }

    @Override
    public ProcessDO getProcess(Long id) {
        return processMapper.selectById(id);
    }

    @Override
    public PageResult<ProcessDO> getProcessPage(ProcessPageReqVO pageReqVO) {
        return processMapper.selectPage(pageReqVO);
    }

    // ================= [新增] 工位矩阵私有方法 =================

    private void createProcessStationList(Long processId, List<ProcessSaveReqVO.ProcessStation> stations) {
        if (CollUtil.isEmpty(stations)) {
            return;
        }
        List<ProcessStationDO> stationDOs = BeanUtils.toBean(stations, ProcessStationDO.class);
        stationDOs.forEach(o -> {
            o.setProcessId(processId);
            // 🛠️ 核心修复：强制清空 ID。
            // 确保在 Delete-Insert 策略下，永远当作新记录插入，让 MP 生成新主键，彻底避免逻辑删除导致的主键冲突！
            o.setId(null);
        });
        processStationMapper.insertBatch(stationDOs);
    }

    private void updateProcessStationList(Long processId, List<ProcessSaveReqVO.ProcessStation> stations) {
        // 策略：先物理删除旧数据，再重新插入新数据 (Simple Delete-Insert Strategy)
        // 优点：代码简单，避免复杂的 Diff 逻辑；缺点：ID 会自增变化，如果需要保持 ID 不变需改用 Diff
        processStationMapper.deleteByProcessId(processId);
        createProcessStationList(processId, stations);
    }

    // [新增] 供 Controller 调用的获取子表方法 (需同步在 Interface 中声明)
    public List<ProcessStationDO> getProcessStationList(Long processId) {
        return processStationMapper.selectListByProcessId(processId);
    }

}
