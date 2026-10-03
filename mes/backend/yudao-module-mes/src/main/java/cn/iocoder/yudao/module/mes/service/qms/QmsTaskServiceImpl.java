// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.qms.QmsTaskServiceImpl.java
package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;



@Service
@Validated
class QmsTaskServiceImpl implements QmsTaskService {

    @Resource
    private QmsTaskMapper qmsTaskMapper;

    @Override
    public Long createQmsTask(QmsTaskSaveReqVO createReqVO) {
        QmsTaskDO taskDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, QmsTaskDO.class);
        if (taskDO.getResult() == null) {
            taskDO.setResult("PENDING"); // 默认待检验
        }
        qmsTaskMapper.insert(taskDO);
        return taskDO.getId();
    }

    @Override
    public void updateQmsTask(QmsTaskSaveReqVO updateReqVO) {
        QmsTaskDO existDO = qmsTaskMapper.selectById(updateReqVO.getId());
        if (existDO == null) {
            throw new RuntimeException("该检验任务不存在！");
        }
        QmsTaskDO updateObj = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(updateReqVO, QmsTaskDO.class);
        qmsTaskMapper.updateById(updateObj);
    }

    @Override
    public PageResult<QmsTaskDO> getQmsTaskPage(QmsTaskPageReqVO pageReqVO) {
        return qmsTaskMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<QmsTaskDO>()
                .likeIfPresent(QmsTaskDO::getTaskNo, pageReqVO.getTaskNo())
                .eqIfPresent(QmsTaskDO::getCheckType, pageReqVO.getCheckType())
                .eqIfPresent(QmsTaskDO::getLotNo, pageReqVO.getLotNo())
                .eqIfPresent(QmsTaskDO::getResult, pageReqVO.getResult())
                .orderByDesc(QmsTaskDO::getId));
    }
}
