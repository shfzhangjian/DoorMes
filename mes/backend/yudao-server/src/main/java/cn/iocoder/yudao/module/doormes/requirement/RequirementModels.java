package cn.iocoder.yudao.module.doormes.requirement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/** Demand is not drawing geometry or the historical MES formula snapshot. */
public final class RequirementModels {
    private RequirementModels() {}
    public static final String INPUT_SCHEMA = "doormes-design-demand.v1";
    public static final String DOCUMENT_SCHEMA = "doormes-design-requirement.v1";

    // Field semantics deliberately match the existing drawing prototype's FactoryRequirement.
    public record Demand(@DecimalMin("1") @DecimalMax("50000") double widthMm,
                         @DecimalMin("1") @DecimalMax("50000") double heightMm,
                         @NotNull @Size(max=100) String material,
                         @NotNull @Size(max=100) String glass,
                         @NotNull @Size(max=100) String hardware,
                         @NotNull @Size(max=100) String finish,
                         @NotNull @Size(max=10) String dueDate,
                         @NotNull @Size(max=2000) String note) {}
    public record Line(@Size(max=36) String id,
                       @NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,39}") String mark,
                       @NotBlank @Pattern(regexp="custom|standard") String kind,
                       @Min(1) @Max(1000) int quantity,
                       @NotNull @Valid Demand requirement) {}
    public record Input(@NotBlank String schemaVersion,
                        @NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,39}") String number,
                        @NotBlank @Size(max=200) String customer,
                        @NotNull @Size(max=200) String project,
                        @NotNull @Size(min=1,max=100) List<@NotNull @Valid Line> lines,
                        @NotNull @Size(max=2000) String note) {}
    public record Mutation(@Min(0) int expectedRevision,
                           @NotBlank @Size(max=1000) String changeNote,
                           @NotNull @Valid Input demand) {}
    public record Action(@Min(1) int expectedRevision, @NotBlank @Size(max=1000) String note) {}
    public record Document(String schemaVersion, String id, long tenantId, int revision,
                           String status, long createdBy, Long assignedTo,
                           String createdAt, String updatedAt, long changedBy,
                           String action, String changeNote, Input demand) {}
    public record Summary(String id, String number, String customer, String project,
                          int revision, String status, long createdBy, Long assignedTo,
                          int lineCount, int quantity, String updatedAt) {}
    public record Version(int revision, String action, String changeNote, long changedBy,
                          String updatedAt) {}
}
