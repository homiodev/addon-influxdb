package org.homio.bundle.influxdb;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.homio.bundle.influxdb.setting.InfluxDBConfigureButtonSetting;
import org.homio.bundle.influxdb.setting.InfluxDBDependencyExecutableInstaller;
import org.springframework.stereotype.Component;
import org.homio.bundle.api.BundleEntrypoint;
import org.homio.bundle.api.EntityContext;

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
