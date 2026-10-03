// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.service.prod.ProdCheckServiceImpl.java
package cn.iocoder.yudao.module.mes.service.prod;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.prod.vo.ProdCheckSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.prod.vo.ProdCheckPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.prod.ProdCheckDO;
import cn.iocoder.yudao.module.mes.dal.mysql.prod.ProdCheckMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

@Service
@Validated
public class ProdCheckServiceImpl implements ProdCheckService {

    @Resource
    private ProdCheckMapper prodCheckMapper;

    @Override
    public Long createProdCheck(ProdCheckSaveReqVO createReqVO) {
        ProdCheckDO checkDO = cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(createReqVO, ProdCheckDO.class);

        if (checkDO.getCheckTime() == null) {
            checkDO.setCheckTime(LocalDateTime.now());
        }

        prodCheckMapper.insert(checkDO);
        return checkDO.getId();
    }

    @Override
    public ProdCheckDO getProdCheck(Long id) {
        return prodCheckMapper.selectById(id);
    }

    @Override
    public PageResult<ProdCheckDO> getProdCheckPage(ProdCheckPageReqVO pageReqVO) {
        return prodCheckMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<ProdCheckDO>()
                .eqIfPresent(ProdCheckDO::getScheduleId, pageReqVO.getScheduleId())
                .eqIfPresent(ProdCheckDO::getCheckType, pageReqVO.getCheckType())
                .eqIfPresent(ProdCheckDO::getPass, pageReqVO.getPass())
                .orderByDesc(ProdCheckDO::getId));
    }
}
