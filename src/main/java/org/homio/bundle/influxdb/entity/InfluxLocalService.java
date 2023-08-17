package org.homio.bundle.influxdb.entity;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.Getter;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.service.EntityService;
import org.homio.bundle.api.ui.UI.Color;

@Getter
public class InfluxLocalService implements EntityService.ServiceInstance<InfluxLocalDBEntity> {

  private InfluxDBClient influxDBClient;
  private final EntityContext entityContext;
  private InfluxLocalDBEntity entity;
  private long hashCode;

  public InfluxLocalService(InfluxLocalDBEntity entity, EntityContext entityContext) {
    this.entity = entity;
    this.entityContext = entityContext;
    this.influxDBClient = InfluxDBClientFactory.create(entity.getUrl(), entity.getUser(), entity.getPassword().asString().toCharArray());
  }

  @Override
  public boolean entityUpdated(InfluxLocalDBEntity entity) {
    long hashCode = entity.getJsonDataHashCode("url", "user", "pwd");
    boolean reconfigure = this.hashCode != hashCode;
    this.hashCode = hashCode;
    this.entity = entity;
    if (reconfigure) {
      this.destroy();
    }
    this.influxDBClient = InfluxDBClientFactory.create(entity.getUrl(), entity.getUser(), entity.getPassword().asString().toCharArray());
    updateNotificationBlock();
    return reconfigure;
  }

  private void updateNotificationBlock() {
    entityContext.ui().addNotificationBlock("influxdb", "InfluxDB", "fab fa-usps", "#90C211", builder -> {
      builder.setStatus(getEntity().getStatus());
      if (!getEntity().getStatus().isOnline()) {
        builder.addInfo(defaultIfEmpty(getEntity().getStatusMessage(), "Unknown error"),
            Color.RED, "fas fa-exclamation", null);
      } else {
        String version = entityContext.hardware().execute("influx -version");
        builder.setVersion(version);
      }
    });
  }

  @Override
  public void destroy() {
    this.influxDBClient.close();
  }

  @Override
  public boolean testService() {
    this.influxDBClient.getUsersApi().findUsers();
    return true;
  }
}
