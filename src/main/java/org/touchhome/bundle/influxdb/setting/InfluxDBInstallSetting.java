package org.touchhome.bundle.influxdb.setting;

import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.setting.SettingPluginButton;

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
