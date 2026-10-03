package cn.iocoder.yudao.module.mes.service.hc.team;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo.HcTeamPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.team.vo.HcTeamSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.team.HcTeamDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.team.HcTeamMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCTEAM_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCTEAM_TEAMCODE_EXISTS;

@Service
@Validated
public class HcTeamServiceImpl implements HcTeamService {

    @Resource
    private HcTeamMapper hcTeamMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcTeam(HcTeamSaveReqVO createReqVO) {
        validateTeamCodeUnique(null, createReqVO.getTeamCode());
        HcTeamDO entity = BeanUtils.toBean(createReqVO, HcTeamDO.class);
        hcTeamMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcTeam(HcTeamSaveReqVO updateReqVO) {
        validateHcTeamExists(updateReqVO.getId());
        validateTeamCodeUnique(updateReqVO.getId(), updateReqVO.getTeamCode());
        HcTeamDO updateObj = BeanUtils.toBean(updateReqVO, HcTeamDO.class);
        hcTeamMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcTeam(Long id) {
        validateHcTeamExists(id);
        hcTeamMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcTeamListByIds(List<Long> ids) {
        hcTeamMapper.deleteByIds(ids);
    }

    private void validateHcTeamExists(Long id) {
        if (hcTeamMapper.selectById(id) == null) {
            throw exception(HCTEAM_NOT_EXISTS);
        }
    }

    private void validateTeamCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcTeamDO entity = hcTeamMapper.selectOne(new LambdaQueryWrapperX<HcTeamDO>().eq(HcTeamDO::getTeamCode, value).neIfPresent(HcTeamDO::getId, id));
        if (entity != null) {
            throw exception(HCTEAM_TEAMCODE_EXISTS);
        }
    }

    @Override
    public HcTeamDO getHcTeam(Long id) {
        return hcTeamMapper.selectById(id);
    }

    @Override
    public List<HcTeamDO> getHcTeamSimpleList() {
        LambdaQueryWrapperX<HcTeamDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcTeamDO::getStatus, 0);
        queryWrapper.orderByAsc(HcTeamDO::getTeamCode);
        queryWrapper.orderByDesc(HcTeamDO::getId);
        return hcTeamMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcTeamDO> getHcTeamList(HcTeamPageReqVO reqVO) {
        return hcTeamMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcTeamDO> getHcTeamPage(HcTeamPageReqVO pageReqVO) {
        return hcTeamMapper.selectPage(pageReqVO);
    }

}