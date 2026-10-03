// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.wms.WmsCarrierServiceImpl.java
package cn.iocoder.yudao.module.mes.service.wms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsCarrierSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsCarrierPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wms.WmsCarrierDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wms.WmsCarrierMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;

@Service
@Validated
public class WmsCarrierServiceImpl implements WmsCarrierService {

    @Resource
    private WmsCarrierMapper wmsCarrierMapper;

    @Override
    public Long createCarrier(WmsCarrierSaveReqVO createReqVO) {
        WmsCarrierDO carrierDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, WmsCarrierDO.class);
        if (carrierDO.getStatus() == null) {
            carrierDO.setStatus("EMPTY"); // 默认空闲
        }
        wmsCarrierMapper.insert(carrierDO);
        return carrierDO.getId();
    }

    @Override
    public void updateCarrier(WmsCarrierSaveReqVO updateReqVO) {
        if (wmsCarrierMapper.selectById(updateReqVO.getId()) == null) {
            throw new RuntimeException("该载具记录不存在！");
        }
        WmsCarrierDO updateObj = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(updateReqVO, WmsCarrierDO.class);
        wmsCarrierMapper.updateById(updateObj);
    }

    @Override
    public void deleteCarrier(Long id) {
        WmsCarrierDO existDO = wmsCarrierMapper.selectById(id);
        if (existDO != null && "OCCUPIED".equals(existDO.getStatus())) {
            throw new RuntimeException("该载具正在被占用，禁止删除！");
        }
        wmsCarrierMapper.deleteById(id);
    }

    @Override
    public WmsCarrierDO getCarrier(Long id) {
        return wmsCarrierMapper.selectById(id);
    }

    @Override
    public PageResult<WmsCarrierDO> getCarrierPage(WmsCarrierPageReqVO pageReqVO) {
        return wmsCarrierMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<WmsCarrierDO>()
                .likeIfPresent(WmsCarrierDO::getCarrierCode, pageReqVO.getCarrierCode())
                .eqIfPresent(WmsCarrierDO::getCarrierType, pageReqVO.getCarrierType())
                .eqIfPresent(WmsCarrierDO::getStatus, pageReqVO.getStatus())
                .likeIfPresent(WmsCarrierDO::getCurrentLocation, pageReqVO.getCurrentLocation())
                .orderByDesc(WmsCarrierDO::getId));
    }
}
