package org.homio.bundle.influxdb.entity;

import com.influxdb.client.domain.Bucket;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.homio.api.model.OptionModel;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.selection.dynamic.DynamicOptionLoader;
import org.homio.api.ui.field.selection.dynamic.DynamicParameterFields;
import org.homio.api.ui.field.selection.dynamic.UIFieldDynamicSelection;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InfluxCloudQueryParameter implements DynamicParameterFields {

  @UIField(order = 110, required = true)
  @UIFieldDynamicSelection(SelectBucket.class)
  public String influxBucket;

  @UIField(order = 130)
  @UIFieldDynamicSelection(value = SelectMeasurement.class, staticParameters = {"_measurement"})
  public Set<String> influxMeasurementFilter;

  @UIField(order = 140)
  @UIFieldDynamicSelection(value = SelectMeasurement.class, staticParameters = {"_field"})
  public Set<String> influxFieldFilters;

  @Override
  public String getGroupName() {
    return "InfluxDB query";
  }

  @Override
  public String getBorderColor() {
    return "#0E7EBC";
  }

  public static class SelectBucket implements DynamicOptionLoader {

    @Override
    public List<OptionModel> loadOptions(DynamicOptionLoaderParameters parameters) {
      List<Bucket> buckets = ((InfluxCloudDBEntity) parameters.getBaseEntity()).getService()
        .getInfluxDBClient().getBucketsApi().findBuckets();
      return buckets.stream().map(b -> OptionModel.key(b.getName())).collect(Collectors.toList());
    }
  }

  public static class SelectMeasurement implements DynamicOptionLoader {

    @Override
    public List<OptionModel> loadOptions(DynamicOptionLoaderParameters parameters) {
      InfluxCloudDBEntity entity = (InfluxCloudDBEntity) parameters.getBaseEntity();
      String query = "from(bucket:\"" + entity.getBucket() + "\")\n" +
                     "  |> range(start:-1y)\n" +
                     "  |> keys()";

      return entity.getService().getInfluxDBClient().getQueryApi()
        .query(query, entity.getOrg())
        .stream()
        .map(FluxTable::getRecords)
        .flatMap(Collection::stream)
        .map(FluxRecord::getValues)
        .map(m -> m.get(parameters.getStaticParameters()[0]))
        .filter(Objects::nonNull)
        .map(Object::toString)
        .distinct()
        .map(OptionModel::key)
        .collect(Collectors.toList());
    }
  }
}
