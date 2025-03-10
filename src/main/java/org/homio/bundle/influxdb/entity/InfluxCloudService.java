package org.homio.bundle.influxdb.entity;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.Getter;
import org.homio.api.Context;
import org.homio.api.service.EntityService;
import org.jetbrains.annotations.Nullable;

@Getter
public class InfluxCloudService extends EntityService.ServiceInstance<InfluxCloudDBEntity> {

  private InfluxDBClient influxDBClient;

  public InfluxCloudService(InfluxCloudDBEntity entity, Context entityContext) {
    super(entityContext, entity, true, "InfluxDB");
  }

  @Override
  protected void initialize() {
    this.influxDBClient = InfluxDBClientFactory.create(entity.getUrl(), entity.getToken().asString().toCharArray());
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
