package org.homio.bundle.influxdb.setting;

import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.setting.SettingPluginButton;

public class InfluxDBInstallSetting implements SettingPluginButton {

  @Override
  public int order() {
    return 100;
  }

  @Override
  public String getIcon() {
    return "fas fa-play";
  }

  @Override
  public boolean isVisible(EntityContext entityContext) {
    return entityContext.getBean(InfluxDBDependencyExecutableInstaller.class)
        .isRequireInstallDependencies(entityContext, true);
  }
}
