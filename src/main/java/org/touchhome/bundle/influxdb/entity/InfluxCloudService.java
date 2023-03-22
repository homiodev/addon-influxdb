package org.touchhome.bundle.influxdb.entity;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import java.util.Objects;
import lombok.Getter;
import org.touchhome.bundle.api.service.EntityService;

@Getter
public class InfluxCloudService implements EntityService.ServiceInstance<InfluxCloudDBEntity> {

  private InfluxDBClient influxDBClient;
  private InfluxCloudDBEntity entity;

  public InfluxCloudService(InfluxCloudDBEntity entity) {
    this.entity = entity;
    this.influxDBClient = InfluxDBClientFactory.create(entity.getUrl(), entity.getToken().asString().toCharArray());
  }

  @Override
  public boolean entityUpdated(InfluxCloudDBEntity entity) {
    boolean updated = false;
    if (!Objects.equals(this.entity.getUrl(), entity.getUrl()) ||
        !Objects.equals(this.entity.getToken().asString(), entity.getToken().asString())) {
      this.influxDBClient.close();
      this.influxDBClient = InfluxDBClientFactory.create(entity.getUrl(), entity.getToken().asString().toCharArray());
    }
    this.entity = entity;
    return updated;
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
