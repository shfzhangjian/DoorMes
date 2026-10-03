package cn.iocoder.yudao.module.mes.service.material;

import cn.iocoder.yudao.module.mes.controller.admin.material.vo.MesMaterialPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.material.vo.MesMaterialSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO;
import cn.iocoder.yudao.module.mes.dal.mysql.material.MesMaterialMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;


import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MES_MATERIAL_NOT_EXISTS;

/**
 * MES物料主数据 Service 实现类
 *
 * @author 演示管理员
 */
@Service
@Validated
public class MesMaterialServiceImpl implements MesMaterialService {

    @Resource
    private MesMaterialMapper mesMaterialMapper;

    @Override
    public Long createMesMaterial(MesMaterialSaveReqVO createReqVO) {
        // 插入
        MesMaterialDO mesMaterial = BeanUtils.toBean(createReqVO, MesMaterialDO.class);
        mesMaterialMapper.insert(mesMaterial);

        // 返回
        return mesMaterial.getId();
    }

    @Override
    public void updateMesMaterial(MesMaterialSaveReqVO updateReqVO) {
        // 校验存在
        validateMesMaterialExists(updateReqVO.getId());
        // 更新
        MesMaterialDO updateObj = BeanUtils.toBean(updateReqVO, MesMaterialDO.class);
        mesMaterialMapper.updateById(updateObj);
    }

    @Override
    public void deleteMesMaterial(Long id) {
        // 校验存在
        validateMesMaterialExists(id);
        // 删除
        mesMaterialMapper.deleteById(id);
    }

    @Override
        public void deleteMesMaterialListByIds(List<Long> ids) {
        // 删除
        mesMaterialMapper.deleteByIds(ids);
        }


    private void validateMesMaterialExists(Long id) {
        if (mesMaterialMapper.selectById(id) == null) {
            throw exception(MES_MATERIAL_NOT_EXISTS);
        }
    }

    @Override
    public MesMaterialDO getMesMaterial(Long id) {
        return mesMaterialMapper.selectById(id);
    }

    @Override
    public PageResult<MesMaterialDO> getMesMaterialPage(MesMaterialPageReqVO pageReqVO) {
        return mesMaterialMapper.selectPage(pageReqVO);
    }

}
