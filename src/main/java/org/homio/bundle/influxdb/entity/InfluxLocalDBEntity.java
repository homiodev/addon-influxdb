package org.homio.bundle.influxdb.entity;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.EntityContextHardware;
import org.homio.bundle.api.entity.types.StorageEntity;
import org.homio.bundle.api.entity.widget.PeriodRequest;
import org.homio.bundle.api.entity.widget.ability.HasTimeValueSeries;
import org.homio.bundle.api.exception.ProhibitedExecution;
import org.homio.bundle.api.model.ActionResponseModel;
import org.homio.bundle.api.model.HasEntityLog;
import org.homio.bundle.api.service.EntityService;
import org.homio.bundle.api.storage.SourceHistory;
import org.homio.bundle.api.storage.SourceHistoryItem;
import org.homio.bundle.api.ui.UISidebarChildren;
import org.homio.bundle.api.ui.field.UIField;
import org.homio.bundle.api.ui.field.UIFieldGroup;
import org.homio.bundle.api.ui.field.action.HasDynamicContextMenuActions;
import org.homio.bundle.api.ui.field.action.UIContextMenuAction;
import org.homio.bundle.api.ui.field.action.v1.UIInputBuilder;
import org.homio.bundle.api.ui.field.selection.dynamic.DynamicParameterFields;
import org.homio.bundle.api.ui.field.selection.dynamic.SelectionWithDynamicParameterFields;
import org.homio.bundle.api.util.CommonUtils;
import org.homio.bundle.api.util.Curl;
import org.homio.bundle.api.util.SecureString;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

@Log4j2
@Getter
@Setter
@Entity
@Accessors(chain = true)
@UISidebarChildren(icon = "fab fa-usps", color = "#90C211")
public class InfluxLocalDBEntity extends StorageEntity<InfluxLocalDBEntity>
    implements HasDynamicContextMenuActions, HasEntityLog,
    HasTimeValueSeries, SelectionWithDynamicParameterFields, EntityService<InfluxLocalService, InfluxLocalDBEntity> {

  public static final String PREFIX = "influxlocaldb_";

  @UIField(order = 1, required = true, inlineEditWhenEmpty = true)
  @UIFieldGroup(value = "SECURITY", order = 10, borderColor = "#23ADAB")
  public String getUser() {
    return getJsonData("user");
  }

  public InfluxLocalDBEntity setUser(String value) {
    setJsonData("user", value);
    return this;
  }

  @UIField(order = 2, required = true, inlineEditWhenEmpty = true)
  @UIFieldGroup("SECURITY")
  public SecureString getPassword() {
    return getJsonSecure("pwd");
  }

  public InfluxLocalDBEntity setPassword(String value) {
    setJsonDataSecure("pwd", value);
    return this;
  }

  @UIField(order = 1, required = true)
  @UIFieldGroup(value = "GENERAL", order = 4)
  public String getBucket() {
    return getJsonData("bucket", "homioBucket");
  }

  public InfluxLocalDBEntity setBucket(String value) {
    setJsonData("bucket", value);
    return this;
  }


  @UIField(order = 2, required = true)
  @UIFieldGroup("GENERAL")
  public String getOrg() {
    return getJsonData("org", "primary");
  }

  public InfluxLocalDBEntity setOrg(String value) {
    setJsonData("org", value);
    return this;
  }

  @UIField(order = 3, required = true, inlineEditWhenEmpty = true)
  @UIFieldGroup("GENERAL")
  public String getUrl() {
    return getJsonData("url", "http://localhost:8086");
  }

  public InfluxLocalDBEntity setUrl(String value) {
    setJsonData("url", value);
    return this;
  }

  @Override
  public String getDefaultName() {
    return "InfluxLocalDB/" + getBucket();
  }

  @Override
  public @NotNull String getEntityPrefix() {
    return PREFIX;
  }

  @Override
  public @NotNull Class<InfluxLocalService> getEntityServiceItemClass() {
    return InfluxLocalService.class;
  }

  @Override
  public InfluxLocalService createService(@NotNull EntityContext entityContext) {
    return new InfluxLocalService(this, entityContext);
  }

  @UIContextMenuAction("CHECK_DB_CONNECTION")
  public ActionResponseModel testConnection() {
    getService().getInfluxDBClient().getUsersApi().findUsers();
    return ActionResponseModel.success();
  }

  private String updateQueryWithFilter(JSONObject parameters, String query, String influxMeasurementFilter,
      String queryFilterKey) {
    JSONArray measurementFilters = parameters.optJSONArray(influxMeasurementFilter);
    if (measurementFilters != null && !measurementFilters.isEmpty()) {
      query += "\n        |> filter(fn: (r) => " + measurementFilters.toList().stream()
                                                                     .map(m -> "r[\"" + queryFilterKey + "\"] == \"" + m + "\"")
                                                                     .collect(Collectors.joining(" or ")) + " )";
    }
    return query;
  }

  @Override
  public void assembleActions(UIInputBuilder uiInputBuilder) {
    /*TODO: String widgetKey = LINE_CHART_WIDGET_PREFIX + "influx_widget";
    if (uiInputBuilder.getEntityContext().getEntity(widgetKey) == null) {
      uiInputBuilder.addSelectableButton("WIDGET.CREATE_LINE_CHART", (entityContext, params) -> {
        entityContext.widget().createLineChartWidget(widgetKey, "InfluxDB query widget",
            builder -> builder.addLineChart(null, InfluxCloudDBEntity.this),
            builder -> {
            }, null);
        // update item to remove dynamic context
        entityContext.ui().updateItem(this);
        return null;
      });
    }*/
  }

  @Override
  public void addUpdateValueListener(EntityContext entityContext, String key, JSONObject dynamicParameters,
      Consumer<Object> listener) {
    throw new RuntimeException("Not implemented yet");
  }

  @Override
  public String getTimeValueSeriesDescription() {
    return "Influx time-value series";
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

    InfluxDBClient influxDB = InfluxDBClientFactory.create(getUrl(), getUser(), getPassword().asString().toCharArray());
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

  @UIContextMenuAction(value = "install_influxdb", icon = "fas fa-play")
  public ActionResponseModel install(EntityContext entityContext) {
    EntityContextHardware hardware = entityContext.hardware();
    if (!isInfluxInstalled(hardware)) {
      entityContext.bgp().runWithProgress("install-influxdb", false, progressBar -> {
        if (SystemUtils.IS_OS_LINUX) {
          hardware.installSoftware("gpg", 120);
          hardware.execute(
              "curl https://repos.influxdata.com/influxdata-archive.key | gpg --dearmor | sudo tee /usr/share/keyrings/influxdb-archive-keyring.gpg "
                  + ">/dev/null");
          hardware.execute("echo \"deb [signed-by=/usr/share/keyrings/influxdb-archive-keyring.gpg] https://repos.influxdata.com/debian $(lsb_release -cs) "
              + "stable\" | sudo tee /etc/apt/sources.list.d/influxdb.list");
          hardware.update()
                  .installSoftware("influxdb", 600)
                  .enableAndStartSystemCtl("influxdb");
        } else {
          Curl.downloadAndExtract("https://dl.influxdata.com/influxdb/releases/influxdb2-2.7.1-windows-amd64.zip",
              "influxdb.zip", progressBar, log);
        }
      }, exception -> {
        if (exception != null) {
          entityContext.ui().sendErrorMessage("Error during install InfluxDb", exception);
        } else {
          entityContext.ui().sendSuccessMessage("InfluxDb installed successfully");
        }
      });
      return ActionResponseModel.showInfo("Installing InfluxDb...");
    } else {
      return ActionResponseModel.showError("InfluxDb already installed");
    }
  }

  private boolean isInfluxInstalled(EntityContextHardware hardware) {
    if (SystemUtils.IS_OS_LINUX) {
      return hardware.isSoftwareInstalled("influxdb");
    } else {
      return Files.isRegularFile(CommonUtils.getInstallPath().resolve("influxdb").resolve("influxdb2-2.7.1-windows-amd64")
                                            .resolve("influx.exe"));
    }
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
  public Object getStatusValue(GetStatusValueRequest request) {
    throw new ProhibitedExecution();
  }

  @Override
  public SourceHistory getSourceHistory(GetStatusValueRequest request) {
    throw new ProhibitedExecution();
  }

  @Override
  public List<SourceHistoryItem> getSourceHistoryItems(GetStatusValueRequest request, int from, int count) {
    throw new ProhibitedExecution();
  }

  @Override
  public String getGetStatusDescription() {
    throw new ProhibitedExecution();
  }
}
