package org.homio.bundle.influxdb.entity;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.Getter;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.service.EntityService;

@Getter
public class InfluxCloudService implements EntityService.ServiceInstance<InfluxCloudDBEntity> {

  private InfluxDBClient influxDBClient;
  private final EntityContext entityContext;
  private InfluxCloudDBEntity entity;
  private long hashCode;

  public InfluxCloudService(InfluxCloudDBEntity entity, EntityContext entityContext) {
    this.entity = entity;
    this.entityContext = entityContext;
    this.influxDBClient = InfluxDBClientFactory.create(entity.getUrl(), entity.getToken().asString().toCharArray());
  }

  @Override
  public boolean entityUpdated(InfluxCloudDBEntity entity) {
    long hashCode = entity.getJsonDataHashCode("url", "token");
    boolean reconfigure = this.hashCode != hashCode;
    this.hashCode = hashCode;
    this.entity = entity;
    if (reconfigure) {
      this.destroy();
    }
    this.influxDBClient = InfluxDBClientFactory.create(entity.getUrl(), entity.getToken().asString().toCharArray());
    return reconfigure;
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
