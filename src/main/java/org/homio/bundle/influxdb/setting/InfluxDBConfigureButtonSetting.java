package org.homio.bundle.influxdb.setting;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.EntityContextUI;
import org.homio.bundle.api.setting.SettingPluginButton;
import org.homio.bundle.api.ui.field.action.ActionInputParameter;
import org.homio.bundle.hquery.hardware.other.MachineHardwareRepository;
import org.homio.bundle.influxdb.entity.InfluxCloudDBEntity;

public class InfluxDBConfigureButtonSetting implements SettingPluginButton {

  public static void configure(EntityContext entityContext) {
    List<ActionInputParameter> inputs = new ArrayList<>();
    inputs.add(ActionInputParameter.message("Request for configure influx db and create influxDB entity"));
    inputs.add(ActionInputParameter.text("Url", "http://localhost:8086"));
    inputs.add(ActionInputParameter.text("Organization", "primary"));
    inputs.add(ActionInputParameter.text("Bucket", "th_bucket"));
    inputs.add(ActionInputParameter.text("User", "th_admin"));
    inputs.add(ActionInputParameter.text("Password", "th_password"));
    inputs.add(ActionInputParameter.text("Token", "th_secret_token"));
    inputs.add(ActionInputParameter.bool("Create InfluxDB entity", true));

    entityContext.ui().sendDialogRequest("create-influx-entity", "Configure influxdb database",
        (responseType, pressedButton, params) -> {
          if (responseType == EntityContextUI.DialogResponseType.Accepted) {
            InfluxCloudDBEntity entity = new InfluxCloudDBEntity()
                .setEntityID(InfluxCloudDBEntity.PREFIX + "influxPrimaryDb")
                .setBucket(params.get("Bucket").asText())
                .setOrg(params.get("Organization").asText())
                .setToken(params.get("Token").asText())
                .setUrl(params.get("Url").asText())
                .setPassword(params.get("Password").asText())
                .setUser(params.get("User").asText());

            String command = String.format(" setup -f -o %s -b %s -p %s -u %s -t %s",
                entity.getOrg(),
                entity.getBucket(),
                entity.getPassword().asString(),
                entity.getUser(),
                entity.getToken().asString());
            entity.setEntityID(InfluxCloudDBEntity.PREFIX + command.hashCode());

            if (params.get("Create InfluxDB entity").asBoolean() && entityContext.getEntity(entity) == null) {
              entityContext.save(entity);
            }

            if (SystemUtils.IS_OS_WINDOWS) {
              String path = entityContext.setting().getValue(InfluxDBPathSetting.class);
              try {
                entityContext.getBean(MachineHardwareRepository.class).execute(path + command);
                entityContext.ui().sendSuccessMessage("InfluxDB configured successfully");
              } catch (Exception ex) {
                entityContext.ui().sendErrorMessage("Error while configure influxDB", "", ex);
              }
            }
          }
        }, dialogBuilder -> dialogBuilder.submitButton("Configure").cancelButton("Discard").group("General", inputs));
  }

  @Override
  public int order() {
    return 120;
  }

  @Override
  public String getIcon() {
    return "fas fa-wrench";
  }

  @Override
  public boolean isDisabled(EntityContext entityContext) {
    return entityContext.getBean(InfluxDBDependencyExecutableInstaller.class)
        .isRequireInstallDependencies(entityContext, true);
  }
}
