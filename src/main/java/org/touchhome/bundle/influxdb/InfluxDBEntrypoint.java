package org.touchhome.bundle.influxdb;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.touchhome.bundle.api.BundleEntrypoint;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.influxdb.setting.InfluxDBConfigureButtonSetting;
import org.touchhome.bundle.influxdb.setting.InfluxDBDependencyExecutableInstaller;

@Log4j2
@Component
@RequiredArgsConstructor
public class InfluxDBEntrypoint implements BundleEntrypoint {

  private final EntityContext entityContext;

  public void init() {
    entityContext.getBean(InfluxDBDependencyExecutableInstaller.class).runDbIfRequire(entityContext);
    entityContext.setting().listenValue(InfluxDBConfigureButtonSetting.class, "influx-listen-configure", () ->
        InfluxDBConfigureButtonSetting.configure(entityContext));
  }

  @Override
  @SneakyThrows
  public void destroy() {
  }

  @Override
  public int order() {
    return 200;
  }

  @Override
  public BundleImageColorIndex getBundleImageColorIndex() {
    return BundleImageColorIndex.ONE;
  }
}
