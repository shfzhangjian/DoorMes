package cn.iocoder.yudao.module.mes.service.unit;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import cn.iocoder.yudao.module.mes.controller.admin.unit.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.unit.UnitDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.mes.dal.mysql.unit.UnitMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES计量单位 Service 实现类
 *
 * @author 演示管理员
 */
@Service
@Validated
public class UnitServiceImpl implements UnitService {

    @Resource
    private UnitMapper unitMapper;

    @Override
    public Long createUnit(UnitSaveReqVO createReqVO) {
        // 插入
        UnitDO unit = BeanUtils.toBean(createReqVO, UnitDO.class);
        unitMapper.insert(unit);

        // 返回
        return unit.getId();
    }

    @Override
    public void updateUnit(UnitSaveReqVO updateReqVO) {
        // 校验存在
        validateUnitExists(updateReqVO.getId());
        // 更新
        UnitDO updateObj = BeanUtils.toBean(updateReqVO, UnitDO.class);
        unitMapper.updateById(updateObj);
    }

    @Override
    public void deleteUnit(Long id) {
        // 校验存在
        validateUnitExists(id);
        // 删除
        unitMapper.deleteById(id);
    }

    @Override
        public void deleteUnitListByIds(List<Long> ids) {
        // 删除
        unitMapper.deleteByIds(ids);
        }


    private void validateUnitExists(Long id) {
        if (unitMapper.selectById(id) == null) {
            throw exception(UNIT_NOT_EXISTS);
        }
    }

    @Override
    public UnitDO getUnit(Long id) {
        return unitMapper.selectById(id);
    }

    @Override
    public PageResult<UnitDO> getUnitPage(UnitPageReqVO pageReqVO) {
        return unitMapper.selectPage(pageReqVO);
    }

}
