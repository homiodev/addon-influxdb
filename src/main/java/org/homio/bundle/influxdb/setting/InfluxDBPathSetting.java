package org.homio.bundle.influxdb.setting;

import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.setting.SettingPluginText;

public class InfluxDBPathSetting implements SettingPluginText {

  @Override
  public int order() {
    return 0;
  }

  @Override
  public boolean isVisible(EntityContext entityContext) {
    return false;
  }

  @Override
  public boolean isDisabled(EntityContext entityContext) {
    return true;
  }
}
