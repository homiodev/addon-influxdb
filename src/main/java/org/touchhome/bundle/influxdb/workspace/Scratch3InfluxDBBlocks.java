package org.touchhome.bundle.influxdb.workspace;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.workspace.WorkspaceBlock;
import org.touchhome.bundle.api.workspace.scratch.MenuBlock;
import org.touchhome.bundle.api.workspace.scratch.Scratch3Block;
import org.touchhome.bundle.api.workspace.scratch.Scratch3ExtensionBlocks;
import org.touchhome.bundle.influxdb.InfluxDBEntrypoint;
import org.touchhome.bundle.influxdb.entity.InfluxCloudDBEntity;

@Log4j2
@Getter
@Component
public class Scratch3InfluxDBBlocks extends Scratch3ExtensionBlocks {

  private final MenuBlock.StaticMenuBlock<TypeEnum> typeMenu;
  private final MenuBlock.ServerMenuBlock influxDbMenu;

  private final Scratch3Block writeCommand;
  private final Scratch3Block appendFieldCommand;
  private final Scratch3Block appendTagCommand;

  public Scratch3InfluxDBBlocks(EntityContext entityContext, InfluxDBEntrypoint influxDBEntrypoint) {
    super("#007190", entityContext, influxDBEntrypoint, null);
    setParent("storage");

    // Menu
    this.influxDbMenu = menuServerItems("influxDbMenu", InfluxCloudDBEntity.class, "Select Influx");
    this.typeMenu = menuStatic("typeMenu", TypeEnum.class, TypeEnum.Float);

    // commands
    this.writeCommand =
        blockCommand(10, "write", "Write [TYPE] [KEY]/[VALUE] as [MEASUREMENT] to [DB]", this::saveCommand);
    this.writeCommand.addArgument("DB", this.influxDbMenu);
    this.writeCommand.addArgument("MEASUREMENT", "sample");
    this.writeCommand.addArgument("KEY", "key");
    this.writeCommand.addArgument(VALUE, 1.0);
    this.writeCommand.addArgument("TYPE", this.typeMenu);

    this.appendFieldCommand = blockCommand(20, InfluxApplyHandler.update_add_field.name(),
        "Field [TYPE] [KEY]/[VALUE]", this::skipExpression);
    this.appendFieldCommand.addArgument("KEY", "key");
    this.appendFieldCommand.addArgument(VALUE, 1.0);
    this.appendFieldCommand.addArgument("TYPE", this.typeMenu);

    this.appendTagCommand = blockCommand(20, InfluxApplyHandler.update_add_tag.name(),
        "Tag [KEY]/[VALUE]", this::skipExpression);
    this.appendTagCommand.addArgument("KEY", "key");
    this.appendTagCommand.addArgument(VALUE, "value");

  }

  private void saveCommand(WorkspaceBlock workspaceBlock) {
    String measurement = workspaceBlock.getInputString("MEASUREMENT");
    String key = workspaceBlock.getInputString("KEY");
    Float value = workspaceBlock.getInputFloat(VALUE);

    InfluxCloudDBEntity influxDBBaseEntity = workspaceBlock.getMenuValueEntityRequired("DB", this.influxDbMenu);
    saveToCloudDb(workspaceBlock, measurement, key, value, influxDBBaseEntity);
  }

  private void saveToCloudDb(WorkspaceBlock workspaceBlock, String measurement, String name, Float value,
      InfluxCloudDBEntity influxCloudDBEntity) {
    com.influxdb.client.write.Point point =
        com.influxdb.client.write.Point.measurement(measurement).time(Instant.now(), WritePrecision.NS);
    point.addField(name, value);

    applyParentBlocks(new ItemBuilder() {
      @Override
      public void addTag(String key, String value) {
        point.addTag(key, value);
      }

      @Override
      public void addField(String key, boolean value) {
        point.addField(key, value);
      }

      @Override
      public void addField(String key, int value) {
        point.addField(key, value);
      }

      @Override
      public void addField(String key, float value) {
        point.addField(key, value);
      }
    }, workspaceBlock.getParent());

    InfluxDBClient influxDBClient = influxCloudDBEntity.getService().getInfluxDBClient();
    try (WriteApi writeApi = influxDBClient.getWriteApi()) {
      writeApi.writePoint(influxCloudDBEntity.getBucket(), influxCloudDBEntity.getOrg(), point);
    }
  }

  @SneakyThrows
  private void applyParentBlocks(ItemBuilder builder, WorkspaceBlock parent) {
    if (parent == null || !parent.getBlockId().startsWith("mail_update_")) {
      return;
    }
    applyParentBlocks(builder, parent.getParent());
    InfluxApplyHandler.valueOf(parent.getOpcode()).applyFn.handle(parent, builder, this);
  }

  private void skipExpression(WorkspaceBlock ignore) {
    // skip expression
  }

  @AllArgsConstructor
  private enum TypeEnum {
    Int((key, value, builder) -> {
      builder.addField(key, value.intValue());
    }), Float((key, value, builder) -> {
      builder.addField(key, value);
    }), Bool((key, value, builder) -> {
      builder.addField(key, value >= 1.0);
    });

    private final TypeAppendHandler applyFn;

    private interface TypeAppendHandler {

      void handle(String key, Float value, ItemBuilder builder) throws Exception;
    }
  }

  @AllArgsConstructor
  private enum InfluxApplyHandler {
    update_add_field((workspaceBlock, builder, scratch) -> {
      TypeEnum menuValue = workspaceBlock.getMenuValue("TYPE", scratch.typeMenu);
      menuValue.applyFn.handle(workspaceBlock.getInputString("KEY"), workspaceBlock.getInputFloat(VALUE), builder);
    }),
    update_add_tag((workspaceBlock, builder, scratch) -> {
      builder.addTag(workspaceBlock.getInputString("KEY"), workspaceBlock.getInputString(VALUE));
    });

    private final InfluxAppendHandler applyFn;

    private interface InfluxAppendHandler {

      void handle(WorkspaceBlock workspaceBlock, ItemBuilder builder, Scratch3InfluxDBBlocks scratch) throws Exception;
    }
  }

  public interface ItemBuilder {

    void addTag(String key, String value);

    void addField(String key, boolean value);

    void addField(String key, int value);

    void addField(String key, float value);
  }
}
