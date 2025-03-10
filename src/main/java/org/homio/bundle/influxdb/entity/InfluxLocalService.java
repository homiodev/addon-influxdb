package org.homio.bundle.influxdb.entity;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.Getter;
import org.homio.api.Context;
import org.homio.api.model.Icon;
import org.homio.api.service.EntityService;
import org.homio.api.ui.UI;
import org.jetbrains.annotations.Nullable;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

@Getter
public class InfluxLocalService extends EntityService.ServiceInstance<InfluxLocalDBEntity> {

  private InfluxDBClient influxDBClient;

  public InfluxLocalService(InfluxLocalDBEntity entity, Context context) {
    super(context, entity, true, "InfluxDB local", true);
  }

  @Override
  protected void initialize() {
    this.influxDBClient = InfluxDBClientFactory.create(entity.getUrl(), entity.getUser(), entity.getPassword().asString().toCharArray());
  }

  public void updateNotificationBlock() {
    context.ui().notification().addBlock("influxdb", "InfluxDB", new Icon("fab fa-usps", "#90C211"), builder -> {
      builder.setStatus(getEntity().getStatus());
      if (!getEntity().getStatus().isOnline()) {
        var err = defaultIfEmpty(getEntity().getStatusMessage(), "Unknown error");
        builder.addInfo(String.valueOf(err.hashCode()),
          new Icon("fas fa-exclamation", UI.Color.RED), err);
      } else {
        String version = context.hardware().execute("influx -version");
        builder.setVersion(version);
      }
    });
  }

  @Override
  public void destroy(boolean forRestart, @Nullable Exception ex) {
    this.influxDBClient.close();
  }

  @Override
  public void testService() {
    this.influxDBClient.getUsersApi().findUsers();
  }
}
