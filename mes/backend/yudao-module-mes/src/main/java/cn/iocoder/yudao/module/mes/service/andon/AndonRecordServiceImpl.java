// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.andon.AndonRecordServiceImpl.java
package cn.iocoder.yudao.module.mes.service.andon;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.andon.vo.AndonRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.andon.vo.AndonRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.andon.AndonRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.andon.AndonRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;

@Service
@Validated
public class AndonRecordServiceImpl implements AndonRecordService {

    @Resource
    private AndonRecordMapper andonRecordMapper;

    @Override
    public Long createAndonRecord(AndonRecordSaveReqVO createReqVO) {
        AndonRecordDO andonDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, AndonRecordDO.class);
        if (andonDO.getStatus() == null) {
            andonDO.setStatus("UNPROCESSED"); // 默认未处理
        }
        andonRecordMapper.insert(andonDO);
        return andonDO.getId();
    }

    @Override
    public void updateAndonRecord(AndonRecordSaveReqVO updateReqVO) {
        if (andonRecordMapper.selectById(updateReqVO.getId()) == null) {
            throw new RuntimeException("该安灯记录不存在！");
        }
        AndonRecordDO updateObj = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(updateReqVO, AndonRecordDO.class);
        andonRecordMapper.updateById(updateObj);
    }

    @Override
    public void deleteAndonRecord(Long id) {
        andonRecordMapper.deleteById(id);
    }

    @Override
    public AndonRecordDO getAndonRecord(Long id) {
        return andonRecordMapper.selectById(id);
    }

    @Override
    public PageResult<AndonRecordDO> getAndonRecordPage(AndonRecordPageReqVO pageReqVO) {
        return andonRecordMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<AndonRecordDO>()
                .likeIfPresent(AndonRecordDO::getAndonNo, pageReqVO.getAndonNo())
                .eqIfPresent(AndonRecordDO::getExceptionType, pageReqVO.getExceptionType())
                .eqIfPresent(AndonRecordDO::getSeverityLevel, pageReqVO.getSeverityLevel())
                .eqIfPresent(AndonRecordDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(AndonRecordDO::getId));
    }
}
