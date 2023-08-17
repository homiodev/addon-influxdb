package org.homio.bundle.influxdb;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.homio.bundle.api.BundleEntrypoint;
import org.homio.bundle.api.EntityContext;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class InfluxDBEntrypoint implements BundleEntrypoint {

  private final EntityContext entityContext;

  public void init() {
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
