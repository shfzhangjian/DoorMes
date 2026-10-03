package cn.iocoder.yudao.module.mes.service.hc.inv.txn.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.txn.vo.HcInvTxnLogPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.txn.HcInvTxnLogMapper;
import cn.iocoder.yudao.module.mes.service.hc.inv.txn.HcInvTxnLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class HcInvTxnLogServiceImpl implements HcInvTxnLogService {

    @Resource
    private HcInvTxnLogMapper hcInvTxnLogMapper;

    @Override
    public PageResult<HcInvTxnLogDO> getInvTxnLogPage(HcInvTxnLogPageReqVO pageReqVO) {
        return hcInvTxnLogMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcInvTxnLogDO> getInvTxnLogList(HcInvTxnLogPageReqVO reqVO) {
        return hcInvTxnLogMapper.selectList(reqVO);
    }

}
