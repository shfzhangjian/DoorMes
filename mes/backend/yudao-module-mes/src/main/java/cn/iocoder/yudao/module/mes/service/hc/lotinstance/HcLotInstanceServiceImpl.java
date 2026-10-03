package cn.iocoder.yudao.module.mes.service.hc.lotinstance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotinstance.vo.HcLotInstancePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotinstance.HcLotInstanceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotinstance.HcLotInstanceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HcLotInstanceServiceImpl implements HcLotInstanceService {

    @Resource
    private HcLotInstanceMapper hcLotInstanceMapper;

    @Override
    public PageResult<HcLotInstanceDO> getLotInstancePage(HcLotInstancePageReqVO pageReqVO) {
        return hcLotInstanceMapper.selectPage(pageReqVO);
    }

    @Override
    public HcLotInstanceDO getLotInstance(Long id) {
        return hcLotInstanceMapper.selectById(id);
    }
}
