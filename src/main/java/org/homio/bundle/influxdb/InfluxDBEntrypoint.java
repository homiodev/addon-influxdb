package org.homio.bundle.influxdb;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.homio.api.AddonConfiguration;
import org.homio.api.AddonEntrypoint;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@AddonConfiguration
@RequiredArgsConstructor
public class InfluxDBEntrypoint implements AddonEntrypoint {

  public void init() {
  }

  @Override
  @SneakyThrows
  public void destroy() {
  }

  @Override
  public @NotNull AddonImageColorIndex getAddonImageColorIndex() {
    return AddonImageColorIndex.ONE;
  }
}
