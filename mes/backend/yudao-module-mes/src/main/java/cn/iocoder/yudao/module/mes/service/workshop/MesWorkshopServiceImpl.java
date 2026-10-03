package cn.iocoder.yudao.module.mes.service.workshop;

import cn.iocoder.yudao.module.mes.controller.admin.workshop.vo.MesWorkshopListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.workshop.vo.MesWorkshopSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workshop.MesWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.mysql.workshop.MesWorkshopMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;


import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;


/**
 * MES车间产线定义 Service 实现类
 *
 * @author 演示管理员
 */
@Service
@Validated
public class MesWorkshopServiceImpl implements MesWorkshopService {

    @Resource
    private MesWorkshopMapper mesWorkshopMapper;

    @Override
    public Long createMesWorkshop(MesWorkshopSaveReqVO createReqVO) {
        // 校验父节点ID的有效性
        validateParentMesWorkshop(null, createReqVO.getParentId());
        // 校验主键ID的唯一性
        //validateMesWorkshopIdUnique(null, createReqVO.getParentId(), createReqVO.getId());

        // 插入
        MesWorkshopDO mesWorkshop = BeanUtils.toBean(createReqVO, MesWorkshopDO.class);
        mesWorkshopMapper.insert(mesWorkshop);

        // 返回
        return mesWorkshop.getId();
    }

    @Override
    public void updateMesWorkshop(MesWorkshopSaveReqVO updateReqVO) {
        // 校验存在
        validateMesWorkshopExists(updateReqVO.getId());
        // 校验父节点ID的有效性
        validateParentMesWorkshop(updateReqVO.getId(), updateReqVO.getParentId());
        // 校验主键ID的唯一性
        //validateMesWorkshopIdUnique(updateReqVO.getId(), updateReqVO.getParentId(), updateReqVO.getId());

        // 更新
        MesWorkshopDO updateObj = BeanUtils.toBean(updateReqVO, MesWorkshopDO.class);
        mesWorkshopMapper.updateById(updateObj);
    }

    @Override
    public void deleteMesWorkshop(Long id) {
        // 校验存在
        validateMesWorkshopExists(id);
        // 校验是否有子MES车间产线定义
        if (mesWorkshopMapper.selectCountByParentId(id) > 0) {
            throw exception(MES_WORKSHOP_EXITS_CHILDREN);
        }
        // 删除
        mesWorkshopMapper.deleteById(id);
    }


    private void validateMesWorkshopExists(Long id) {
        if (mesWorkshopMapper.selectById(id) == null) {
            throw exception(MES_WORKSHOP_NOT_EXISTS);
        }
    }

    private void validateParentMesWorkshop(Long id, Long parentId) {
        if (parentId == null || MesWorkshopDO.PARENT_ID_ROOT.equals(parentId)) {
            return;
        }
        // 1. 不能设置自己为父MES车间产线定义
        if (Objects.equals(id, parentId)) {
            throw exception(MES_WORKSHOP_PARENT_ERROR);
        }
        // 2. 父MES车间产线定义不存在
        MesWorkshopDO parentMesWorkshop = mesWorkshopMapper.selectById(parentId);
        if (parentMesWorkshop == null) {
            throw exception(MES_WORKSHOP_PARENT_NOT_EXITS);
        }
        // 3. 递归校验父MES车间产线定义，如果父MES车间产线定义是自己的子MES车间产线定义，则报错，避免形成环路
        if (id == null) { // id 为空，说明新增，不需要考虑环路
            return;
        }
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            // 3.1 校验环路
            parentId = parentMesWorkshop.getParentId();
            if (Objects.equals(id, parentId)) {
                throw exception(MES_WORKSHOP_PARENT_IS_CHILD);
            }
            // 3.2 继续递归下一级父MES车间产线定义
            if (parentId == null || MesWorkshopDO.PARENT_ID_ROOT.equals(parentId)) {
                break;
            }
            parentMesWorkshop = mesWorkshopMapper.selectById(parentId);
            if (parentMesWorkshop == null) {
                break;
            }
        }
    }

//    private void validateMesWorkshopIdUnique(Long id, Long parentId, String id) {
//        MesWorkshopDO mesWorkshop = mesWorkshopMapper.selectByParentIdAndId(parentId, id);
//        if (mesWorkshop == null) {
//            return;
//        }
//        // 如果 id 为空，说明不用比较是否为相同 id 的MES车间产线定义
//        if (id == null) {
//            throw exception(MES_WORKSHOP_ID_DUPLICATE);
//        }
//        if (!Objects.equals(mesWorkshop.getId(), id)) {
//            throw exception(MES_WORKSHOP_ID_DUPLICATE);
//        }
//    }

    @Override
    public MesWorkshopDO getMesWorkshop(Long id) {
        return mesWorkshopMapper.selectById(id);
    }

    @Override
    public List<MesWorkshopDO> getMesWorkshopList(MesWorkshopListReqVO listReqVO) {
        return mesWorkshopMapper.selectList(listReqVO);
    }

}
