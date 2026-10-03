// 完整路径: cn.iocoder.yudao.module.mes.service.routeprocess.RouteProcessServiceImpl
package cn.iocoder.yudao.module.mes.service.routeprocess;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.route.vo.RouteRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessDO;
// [新增] 引入 4M1E 相关的 DO
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessInputDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessPostDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessSopDO;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessMapper;
// [新增] 引入 4M1E 相关的 Mapper
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessInputMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessPostMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessSopMapper;

import cn.iocoder.yudao.module.mes.service.routeprocessparam.RouteProcessParamService;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Service
@Validated
public class RouteProcessServiceImpl implements RouteProcessService {

    @Resource
    private RouteProcessMapper routeProcessMapper;

    // [原有] 参数服务 (Environment)
    @Resource
    private RouteProcessParamService routeProcessParamService;

    // [新增] 投料 Mapper (Material)
    @Resource
    private RouteProcessInputMapper routeProcessInputMapper;

    // [新增] 岗位 Mapper (Man)
    @Resource
    private RouteProcessPostMapper routeProcessPostMapper;

    // [新增] SOP Mapper (Method)
    @Resource
    private RouteProcessSopMapper routeProcessSopMapper;

    @Override
    public void createRouteProcessList(Long routeId, List<RouteSaveReqVO.RouteProcess> list) {
        if (CollUtil.isEmpty(list)) return;

        for (RouteSaveReqVO.RouteProcess vo : list) {
            // 1. 转为 DO 保存子表 (RouteProcess)
            RouteProcessDO routeProcess = BeanUtils.toBean(vo, RouteProcessDO.class);
            routeProcess.setRouteId(routeId);
            routeProcessMapper.insert(routeProcess);

            // 2. [级联] 保存孙表 - 4M1E
            Long rpId = routeProcess.getId();

            // 2.1 Environment (Param) - 使用原有 Service
            routeProcessParamService.createParamList(routeId, rpId, vo.getParams());

            // 2.2 Material (Input) - [新增]
            if (CollUtil.isNotEmpty(vo.getInputs())) {
                List<RouteProcessInputDO> inputs = BeanUtils.toBean(vo.getInputs(), RouteProcessInputDO.class);
                inputs.forEach(o -> o.setRouteProcessId(rpId));
                routeProcessInputMapper.insertBatch(inputs);
            }

            // 2.3 Man (Post) - [新增]
            if (CollUtil.isNotEmpty(vo.getPosts())) {
                List<RouteProcessPostDO> posts = BeanUtils.toBean(vo.getPosts(), RouteProcessPostDO.class);
                posts.forEach(o -> o.setRouteProcessId(rpId));
                routeProcessPostMapper.insertBatch(posts);
            }

            // 2.4 Method (SOP) - [新增]
            if (CollUtil.isNotEmpty(vo.getSops())) {
                List<RouteProcessSopDO> sops = BeanUtils.toBean(vo.getSops(), RouteProcessSopDO.class);
                sops.forEach(o -> o.setRouteProcessId(rpId));
                routeProcessSopMapper.insertBatch(sops);
            }
        }
    }

    @Override
    public void updateRouteProcessList(Long routeId, List<RouteSaveReqVO.RouteProcess> list) {
        if (list == null) list = new ArrayList<>();

        // 1. 获取旧数据，处理删除 (Diff 逻辑：对比 ID)
        List<RouteProcessDO> oldList = routeProcessMapper.selectListByRouteId(routeId);

        Set<Long> updateIds = new HashSet<>();
        for (RouteSaveReqVO.RouteProcess vo : list) {
            if (vo.getId() != null) updateIds.add(vo.getId());
        }

        List<Long> deleteIds = new ArrayList<>();
        for (RouteProcessDO old : oldList) {
            if (!updateIds.contains(old.getId())) deleteIds.add(old.getId());
        }

        // 执行删除
        if (CollUtil.isNotEmpty(deleteIds)) {
            routeProcessMapper.deleteBatchIds(deleteIds);
            // [级联删除] 孙表数据
            deleteIds.forEach(pid -> {
                routeProcessParamService.deleteByRouteProcessId(pid); // Param
                routeProcessInputMapper.deleteByRouteProcessId(pid); // Input
                routeProcessPostMapper.deleteByRouteProcessId(pid);  // Post
                routeProcessSopMapper.deleteByRouteProcessId(pid);   // Sop
            });
        }

        // 2. 处理新增和修改
        for (RouteSaveReqVO.RouteProcess vo : list) {
            RouteProcessDO processDO = BeanUtils.toBean(vo, RouteProcessDO.class);
            processDO.setRouteId(routeId);

            if (vo.getId() == null) {
                routeProcessMapper.insert(processDO);
            } else {
                routeProcessMapper.updateById(processDO);
            }

            Long rpId = processDO.getId();

            // 3. [级联] 更新孙表 - 策略：先删后加 (Simple Replace)

            // 3.1 Param
            routeProcessParamService.updateParamList(routeId, rpId, vo.getParams());

            // 3.2 Input
            routeProcessInputMapper.deleteByRouteProcessId(rpId);
            if (CollUtil.isNotEmpty(vo.getInputs())) {
                List<RouteProcessInputDO> inputs = BeanUtils.toBean(vo.getInputs(), RouteProcessInputDO.class);
                inputs.forEach(o -> o.setRouteProcessId(rpId));
                routeProcessInputMapper.insertBatch(inputs);
            }

            // 3.3 Post
            routeProcessPostMapper.deleteByRouteProcessId(rpId);
            if (CollUtil.isNotEmpty(vo.getPosts())) {
                List<RouteProcessPostDO> posts = BeanUtils.toBean(vo.getPosts(), RouteProcessPostDO.class);
                posts.forEach(o -> o.setRouteProcessId(rpId));
                routeProcessPostMapper.insertBatch(posts);
            }

            // 3.4 SOP
            routeProcessSopMapper.deleteByRouteProcessId(rpId);
            if (CollUtil.isNotEmpty(vo.getSops())) {
                List<RouteProcessSopDO> sops = BeanUtils.toBean(vo.getSops(), RouteProcessSopDO.class);
                sops.forEach(o -> o.setRouteProcessId(rpId));
                routeProcessSopMapper.insertBatch(sops);
            }
        }
    }

    @Override
    public void deleteByRouteId(Long routeId) {
        List<RouteProcessDO> list = routeProcessMapper.selectListByRouteId(routeId);
        if (CollUtil.isNotEmpty(list)) {
            routeProcessMapper.deleteByRouteId(routeId);

            // [级联删除] 孙表
            // 优化：如果各孙表有 routeId 冗余字段 (V3.1 SQL中没有强制要求，但 Input/Post/Sop 只有 routeProcessId)，
            // 因此必须先查出 ID 再循环删，或者使用 JOIN 删除。这里采用循环删除，逻辑最清晰。
            list.forEach(rp -> {
                Long rpId = rp.getId();
                routeProcessParamService.deleteByRouteProcessId(rpId);
                routeProcessInputMapper.deleteByRouteProcessId(rpId);
                routeProcessPostMapper.deleteByRouteProcessId(rpId);
                routeProcessSopMapper.deleteByRouteProcessId(rpId);
            });
        }
    }

    @Override
    public void deleteByRouteIds(Collection<Long> routeIds) {
        if (CollUtil.isEmpty(routeIds)) return;
        routeIds.forEach(this::deleteByRouteId);
    }

    @Override
    public List<RouteRespVO.RouteProcess> getProcessListByRouteId(Long routeId) {
        // 1. 查询工序节点
        List<RouteProcessDO> dos = routeProcessMapper.selectListByRouteId(routeId);
        List<RouteRespVO.RouteProcess> vos = BeanUtils.toBean(dos, RouteRespVO.RouteProcess.class);

        // 2. 填充 4M1E 详情
        for (RouteRespVO.RouteProcess vo : vos) {
            Long rpId = vo.getId();

            // Environment
            vo.setParams(routeProcessParamService.getParamVOListByRouteProcessId(rpId));

            // Material
            List<RouteProcessInputDO> inputs = routeProcessInputMapper.selectListByRouteProcessId(rpId);
            vo.setInputs(BeanUtils.toBean(inputs, RouteRespVO.RouteProcessInput.class));

            // Man
            List<RouteProcessPostDO> posts = routeProcessPostMapper.selectListByRouteProcessId(rpId);
            vo.setPosts(BeanUtils.toBean(posts, RouteRespVO.RouteProcessPost.class));

            // Method
            List<RouteProcessSopDO> sops = routeProcessSopMapper.selectListByRouteProcessId(rpId);
            vo.setSops(BeanUtils.toBean(sops, RouteRespVO.RouteProcessSop.class));
        }

        // 按 sequence 排序
        vos.sort(Comparator.comparingInt(RouteRespVO.RouteProcess::getSeqNo));
        return vos;
    }
}
