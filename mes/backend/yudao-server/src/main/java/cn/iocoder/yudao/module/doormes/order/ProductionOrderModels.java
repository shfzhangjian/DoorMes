package cn.iocoder.yudao.module.doormes.order;

import cn.iocoder.yudao.module.doormes.drawing.DrawingModels;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/** An order freezes a saved design version; design BOM is not a released cutting instruction. */
public final class ProductionOrderModels {
    private ProductionOrderModels() {}
    public static final String SCHEMA = "doormes-production-order.v1";
    public record Custom(@NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,39}") String mark,
        @DecimalMin("1") @DecimalMax("50000") double widthMm,@DecimalMin("1") @DecimalMax("50000") double heightMm,
        @NotNull @Size(max=100) String material,@NotNull @Size(max=100) String glass,
        @NotNull @Size(max=100) String hardware,@NotNull @Size(max=100) String finish,
        @NotNull @Size(max=10) String dueDate,@NotNull @Size(max=2000) String note) {}
    public record Create(@NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,39}") String number,
        @NotBlank @Size(max=200) String customer,@NotNull @Size(max=200) String project,
        @NotBlank @Pattern(regexp="STANDARD|CUSTOM") String type,@Min(1) @Max(1000) int quantity,
        @NotNull @Size(max=2000) String note,String drawingId,Integer drawingRevision,@Valid Custom custom) {}
    public record Update(@Min(1) int expectedRevision,
        @NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,39}") String number,
        @NotBlank @Size(max=200) String customer,@NotNull @Size(max=200) String project,
        @NotBlank @Pattern(regexp="CUSTOM") String type,@Min(1) @Max(1000) int quantity,
        @NotNull @Size(max=2000) String note,@NotNull @Valid Custom custom) {}
    public record Action(@Min(1) int expectedRevision,@NotBlank @Size(max=1000) String note) {}
    public record Bind(@Min(1) int expectedRevision,@NotBlank String drawingId,@Min(1) int drawingRevision,
        @NotBlank @Size(max=1000) String note) {}
    public record RequestChange(@Min(1) int expectedRevision,@NotBlank String drawingId,@Min(1) int drawingRevision,
        @NotBlank @Size(max=1000) String reason) {}
    public record Review(@Min(1) int expectedRevision,@NotBlank String changeId,boolean approved,
        @NotBlank @Size(max=1000) String note) {}
    public record Apply(@Min(1) int expectedRevision,@NotBlank String changeId,@NotBlank @Size(max=1000) String note) {}
    public record RequirementRef(String id,int revision,String lineId) {}
    public record DrawingRef(String id,int revision,String number,String name,String sha256) {}
    public record ChangedLine(JsonNode before,JsonNode after) {}
    public record Diff(List<JsonNode> added,List<JsonNode> removed,List<ChangedLine> modified) {}
    public record Change(String id,String status,String reason,DrawingRef from,DrawingRef to,Diff diff,
        JsonNode targetBom,long requestedBy,String requestedAt,Long reviewedBy,String reviewNote,String reviewedAt,
        Long appliedBy,String appliedAt) {}
    public record Order(String schemaVersion,String id,long tenantId,int revision,String number,String customer,
        String project,String type,int quantity,String note,String status,RequirementRef requirement,DrawingRef drawing,
        JsonNode bom,List<Change> changes,boolean productionReady,long createdBy,Long assignedTo,
        String createdAt,String updatedAt,long changedBy,String action,String changeNote) {}
    public record Version(int revision,String action,String note,long changedBy,String updatedAt) {}
    public record Summary(String id,int revision,String number,String customer,String project,String type,int quantity,
        String note,String status,RequirementRef requirement,DrawingRef drawing,boolean productionReady,
        long createdBy,Long assignedTo,String updatedAt,String changeStatus,int changeCount) {}
    public record OpenResult(Order order,DrawingModels.Snapshot drawing) {}
}
