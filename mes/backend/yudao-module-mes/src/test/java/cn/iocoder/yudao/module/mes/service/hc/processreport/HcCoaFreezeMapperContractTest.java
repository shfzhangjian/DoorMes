package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedPackagingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HcCoaFreezeMapperContractTest {
    @Test void changedMapperScriptsParse() {
        XMLLanguageDriver driver = new XMLLanguageDriver();
        for (Class<?> mapper : new Class<?>[]{HcFinishedPackagingMapper.class, HcInnerPackUnitMapper.class, QmsFaiOrderMapper.class}) {
            for (var method : mapper.getDeclaredMethods()) {
                Select select = method.getAnnotation(Select.class);
                if (select == null) continue;
                String sql = String.join("\n", select.value());
                assertDoesNotThrow(() -> driver.createSqlSource(new Configuration(), sql, java.util.Map.class), method.toString());
            }
        }
    }
}
