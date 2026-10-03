package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@TableName(value = "mes_qms_exception_flow_log", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsExceptionFlowLogDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long exceptionId;
    private String exceptionNo;
    private String actionCode;
    private String actionName;
    private String fromStatus;
    private String toStatus;
    private String fromNodeCode;
    private String fromNodeName;
    private String toNodeCode;
    private String toNodeName;
    private String opinion;
    private Long handlerUserId;
    private String handlerUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> businessSnapshot;

    private Long tenantId;
}
