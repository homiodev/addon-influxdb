package org.homio.bundle.influxdb.entity;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.service.spi.ServiceException;
import org.homio.api.Context;
import org.homio.api.entity.device.DeviceBaseEntity;
import org.homio.api.entity.log.HasEntityLog;
import org.homio.api.entity.widget.PeriodRequest;
import org.homio.api.entity.widget.ability.HasTimeValueSeries;
import org.homio.api.model.ActionResponseModel;
import org.homio.api.service.EntityService;
import org.homio.api.state.State;
import org.homio.api.storage.SourceHistory;
import org.homio.api.storage.SourceHistoryItem;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.UIFieldType;
import org.homio.api.ui.field.action.HasDynamicContextMenuActions;
import org.homio.api.ui.field.action.UIContextMenuAction;
import org.homio.api.ui.field.action.v1.UIInputBuilder;
import org.homio.api.ui.field.selection.dynamic.DynamicParameterFields;
import org.homio.api.ui.field.selection.dynamic.SelectionWithDynamicParameterFields;
import org.homio.api.ui.route.UIRouteStorage;
import org.homio.api.util.Lang;
import org.homio.api.util.SecureString;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@SuppressWarnings({"JpaAttributeMemberSignatureInspection", "JpaAttributeTypeInspection", "unused"})
@Log4j2
@Getter
@Setter
@Entity
@Accessors(chain = true)
@UIRouteStorage(icon = "fab fa-cloudflare", color = "#90C211")
public class InfluxCloudDBEntity extends DeviceBaseEntity implements
  HasDynamicContextMenuActions,
  HasEntityLog,
  HasTimeValueSeries,
  SelectionWithDynamicParameterFields,
  EntityService<InfluxCloudService> {

  @UIField(order = 1, hideInEdit = true, hideOnEmpty = true, fullWidth = true, bg = "#334842", type = UIFieldType.HTML)
  public final String getDescription() {
    return Lang.getServerMessage(getDescription(isRequireConfigure()));
  }

  public boolean isRequireConfigure() {
    return isEmpty(getToken()) || isEmpty(getUrl()) || isEmpty(getBucket());
  }

  public String getDescription(boolean require) {
    return require ? "influxclouddb.require_description" : "influxclouddb.description";
  }

  @UIField(order = 30, required = true, inlineEditWhenEmpty = true)
  public SecureString getToken() {
    return getJsonSecure("token");
  }

  public InfluxCloudDBEntity setToken(String value) {
    setJsonDataSecure("token", value);
    return this;
  }

  @UIField(order = 40)
  public String getBucket() {
    return getJsonData("bucket", "homioBucket");
  }

  public InfluxCloudDBEntity setBucket(String value) {
    setJsonData("bucket", value);
    return this;
  }

  @UIField(order = 40)
  public String getOrg() {
    return getJsonData("org", "primary");
  }

  public InfluxCloudDBEntity setOrg(String value) {
    setJsonData("org", value);
    return this;
  }

  @UIField(order = 100, required = true, inlineEditWhenEmpty = true)
  public String getUrl() {
    return getJsonData("url", "https://eu-central-1-1.aws.cloud2.influxdata.com");
  }

  public InfluxCloudDBEntity setUrl(String value) {
    setJsonData("url", value);
    return this;
  }

  @Override
  public String getDefaultName() {
    if (StringUtils.isEmpty(getBucket())) {
      return "InfluxCloudDB";
    }
    return "InfluxCloudDB/" + getBucket();
  }

  @Override
  protected @NotNull String getDevicePrefix() {
    return "influxclouddb";
  }

  @Override
  public long getEntityServiceHashCode() {
    return getJsonDataHashCode("url", "bucket", "org", "token");
  }

  @Override
  public InfluxCloudService createService(@NotNull Context context) {
    return new InfluxCloudService(this, context);
  }

  @UIContextMenuAction("CHECK_DB_CONNECTION")
  public ActionResponseModel testConnection() {
    getService().getInfluxDBClient().getUsersApi().findUsers();
    return ActionResponseModel.success();
  }

  private String updateQueryWithFilter(JSONObject parameters, String query, String influxMeasurementFilter,
                                       String queryFilterKey) {
    var measurementFilters = parameters.optJSONArray(influxMeasurementFilter);
    if (measurementFilters != null && !measurementFilters.isEmpty()) {
      query += "\n        |> filter(fn: (r) => " + measurementFilters.toList().stream()
        .map(m -> "r[\"" + queryFilterKey + "\"] == \"" + m + "\"")
        .collect(Collectors.joining(" or ")) + " )";
    }
    return query;
  }

  @Override
  public @NotNull Map<TimeValueDatasetDescription, List<Object[]>> getMultipleTimeValueSeries(PeriodRequest request) {
    String bucket = request.getParameters().optString("influxBucket");

    if (StringUtils.isEmpty(bucket)) {
      return Collections.emptyMap();
    }

    // range
    String query = "from(bucket:\"" + bucket + "\")\n";
    if (request.getFrom() == null) {
      throw new IllegalArgumentException("Unable to filter without specify FROM date");
    }
    if (request.getTo() == null) {
      query += "        |> range(start: " + request.getFromTime() + ")";
    } else {
      query += "        |> range(start: " + request.getFromTime() + ", stop: " +
               request.getToTime() + ")";
    }

    query = updateQueryWithFilter(request.getParameters(), query, "influxMeasurementFilter", "_measurement");
    query = updateQueryWithFilter(request.getParameters(), query, "influxFieldFilters", "_field");

    InfluxDBClient influxDB = InfluxDBClientFactory.create(getUrl(), getToken().asString().toCharArray());
    List<FluxTable> tables = influxDB.getQueryApi().query(query, getOrg());

    Map<TimeValueDatasetDescription, List<Object[]>> charts = new HashMap<>(tables.size());
    for (FluxTable fluxTable : tables) {
      List<Object[]> values = new ArrayList<>(fluxTable.getRecords().size());

      List<FluxRecord> records = fluxTable.getRecords();
      for (FluxRecord fluxRecord : records) {
        values.add(new Object[]{Objects.requireNonNull(fluxRecord.getTime()).toEpochMilli(),
          ((Double) Objects.requireNonNull(fluxRecord.getValue())).floatValue(), fluxRecord.getMeasurement()});
      }

      charts.put(new TimeValueDatasetDescription(Integer.toString(fluxTable.toString().hashCode())), values);
    }
    influxDB.close();
    return charts;
  }

  @Override
  public @NotNull List<Object[]> getTimeValueSeries(@NotNull PeriodRequest request) {
    throw new ServiceException("Not implemented yet");
  }

  @Override
  public DynamicParameterFields getDynamicParameterFields(RequestDynamicParameter request) {
    return new InfluxCloudQueryParameter(getBucket(), Collections.singleton("r[\"_measurement\"] == \"sample\")"),
      Collections.singleton("r[\"_field\"] == \"test\""));
  }

  @Override
  public void logBuilder(EntityLogBuilder entityLogBuilder) {
    entityLogBuilder.addTopic("com.influxdb");
    entityLogBuilder.addTopic("org.homio.bundle.influxdb");
  }

  /**
   * Not implemented yet
   */
  @Override
  public Object getStatusValue(@NotNull GetStatusValueRequest request) {
    throw new ServiceException("Not implemented yet");
  }

  @Override
  public ValueType getValueType() {
    return ValueType.Float;
  }

  @Override
  public SourceHistory getSourceHistory(@NotNull GetStatusValueRequest request) {
    throw new ServiceException("Not implemented yet");
  }

  @Override
  public List<SourceHistoryItem> getSourceHistoryItems(@NotNull GetStatusValueRequest request, int from, int count) {
    throw new ServiceException("Not implemented yet");
  }

  @Override
  public void assembleActions(UIInputBuilder uiInputBuilder) {

  }

  @Override
  public @NotNull UpdateValueListener addUpdateValueListener(@NotNull Context context, @NotNull String discriminator, @NotNull Duration ttl, @NotNull JSONObject dynamicParameters, @NotNull Consumer<State> listener) {
    throw new ServiceException("Not implemented yet");
  }
}
