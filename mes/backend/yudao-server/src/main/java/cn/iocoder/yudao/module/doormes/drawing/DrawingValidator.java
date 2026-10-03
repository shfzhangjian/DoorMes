package cn.iocoder.yudao.module.doormes.drawing;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** Runs the SAME command-replay validator as the UI, with bounded JSON-only input. */
@Component @Profile("doormes")
public class DrawingValidator {
    private final Path node,script,temporaryRoot;
    private final ObjectMapper mapper;
    private final Semaphore slots=new Semaphore(2);
    public DrawingValidator(@Value("${doormes.drawings.node}") String node,
        @Value("${doormes.drawings.validator}") String script,
        @Value("${doormes.drawings.temporary-root}") String temporaryRoot,ObjectMapper mapper) {
        this.node=Path.of(node).toAbsolutePath().normalize();this.script=Path.of(script).toAbsolutePath().normalize();
        this.temporaryRoot=Path.of(temporaryRoot).toAbsolutePath().normalize();this.mapper=mapper;
    }
    public JsonNode validate(JsonNode document) { return run(Map.of("mode","validate","document",document)); }
    public JsonNode bom(JsonNode document) { return run(Map.of("mode","bom","document",document)); }
    public JsonNode catalog(String id,int revision,Object input){return run(Map.of("mode","catalog","id",id,"revision",revision,"input",input));}
    public JsonNode inspectAsset(byte[] payload,String hash){return run(Map.of("mode","asset-inspect","expectedContentHash",hash),payload);}
    public JsonNode seed(String id,String lineId,String mark,int quantity,double width,double height) {
        return run(Map.of("mode","seed","input",Map.of("designId",id,"lineId",lineId,"mark",mark,"quantity",quantity,"widthMm",width,"heightMm",height)));
    }
    private JsonNode run(Object request) { return run(request,null); }
    private JsonNode run(Object request,byte[] payload) {
        boolean acquired=false;Path directory=null;Process process=null;
        try {
            byte[] bytes=mapper.writeValueAsBytes(request);
            if(bytes.length>5_000_000) throw new ServiceException(400,"图纸 JSON 超过 5MB，请拆分设计");
            if(!Files.isRegularFile(node) || !Files.isRegularFile(script)) throw new ServiceException(503,"绘图校验服务未部署，不能绕过校验保存");
            acquired=slots.tryAcquire(3,TimeUnit.SECONDS);
            if(!acquired) throw new ServiceException(503,"绘图校验繁忙，请稍后重试");
            DrawingFiles.safe(temporaryRoot,temporaryRoot.resolve("probe"));
            Files.createDirectories(temporaryRoot);
            directory=Files.createTempDirectory(temporaryRoot,"validate-");
            Path input=directory.resolve("input.json"),output=directory.resolve("output.json");
            if(payload!=null){
                Path content=directory.resolve("payload.bin");Files.write(content,payload,StandardOpenOption.CREATE_NEW);
                var metadata=mapper.valueToTree(request);((com.fasterxml.jackson.databind.node.ObjectNode)metadata).put("payloadPath",content.toString());bytes=mapper.writeValueAsBytes(metadata);
            }
            Files.write(input,bytes,StandardOpenOption.CREATE_NEW);
            process=new ProcessBuilder(node.toString(),script.toString(),input.toString(),output.toString())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if(!process.waitFor(15,TimeUnit.SECONDS)) throw new ServiceException(503,"绘图校验超时，请简化图纸后重试");
            if(!Files.isRegularFile(output) || Files.size(output)>6_000_000) throw new ServiceException(400,"绘图数据无效或超过复杂度限制");
            JsonNode result=mapper.readTree(Files.readAllBytes(output));
            if(process.exitValue()!=0 || !result.path("ok").asBoolean() || !result.path("document").isObject())
                throw new ServiceException(400,"绘图校验失败，请检查尺寸、分格、编号及连接关系；原版本未改变");
            return result.get("document");
        } catch(InterruptedException e) {Thread.currentThread().interrupt();throw new ServiceException(503,"绘图校验中断");}
        catch(java.io.IOException e) {throw new ServiceException(503,"绘图校验服务不可用，原版本未改变");}
        finally {
            if(process!=null && process.isAlive()) {process.destroyForcibly();try {process.waitFor(3,TimeUnit.SECONDS);} catch(InterruptedException e){Thread.currentThread().interrupt();}}
            if(directory!=null) try {Files.deleteIfExists(directory.resolve("input.json"));Files.deleteIfExists(directory.resolve("output.json"));Files.deleteIfExists(directory.resolve("payload.bin"));Files.deleteIfExists(directory);} catch(java.io.IOException ignored) {}
            if(acquired) slots.release();
        }
    }
}
