package org.touchhome.bundle.influxdb.setting;

import org.touchhome.bundle.api.setting.SettingPluginBoolean;

public class InfluxDBRunAtStartupSetting implements SettingPluginBoolean {

  @Override
  public int order() {
    return 100;
  }

  @Override
  public boolean defaultValue() {
    return true;
  }
}
