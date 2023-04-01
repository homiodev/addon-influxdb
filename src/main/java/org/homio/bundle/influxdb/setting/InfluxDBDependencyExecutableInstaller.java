package org.homio.bundle.influxdb.setting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.entity.dependency.DependencyExecutableInstaller;
import org.homio.bundle.api.setting.SettingPluginButton;
import org.homio.bundle.api.setting.SettingPluginText;
import org.homio.bundle.api.util.CommonUtils;
import org.homio.bundle.api.ui.field.ProgressBar;
import org.homio.bundle.hquery.hardware.other.MachineHardwareRepository;

@Log4j2
@Component
public class InfluxDBDependencyExecutableInstaller extends DependencyExecutableInstaller {

  @Override
  public String getName() {
    return "influx";
  }

  @Override
  public Path installDependencyInternal(@NotNull EntityContext entityContext, @NotNull ProgressBar progressBar) {
    if (SystemUtils.IS_OS_LINUX) {
      MachineHardwareRepository machineHardwareRepository = entityContext.getBean(MachineHardwareRepository.class);
      machineHardwareRepository.execute(
          "curl -s https://repos.influxdata.com/influxdb.key | gpg --dearmor > /etc/apt/trusted.gpg.d/influxdb.gpg\n" +
              "export DISTRIB_ID=$(lsb_release -si); export DISTRIB_CODENAME=$(lsb_release -sc)\n" +
              "echo \"deb [signed-by=/etc/apt/trusted.gpg.d/influxdb.gpg] https://repos.influxdata" +
              ".com/${DISTRIB_ID,,} ${DISTRIB_CODENAME} stable\" > /etc/apt/sources.list.d/influxdb.list");
      machineHardwareRepository.installSoftware("influxdb", 600);
      machineHardwareRepository.execute("sudo service influxdb start");
      return null;
    } else {
      Path targetFolder;
      if (Files.isRegularFile(CommonUtils.getInstallPath().resolve("influxdb").resolve("influxdb2-2.0.6-windows-amd64")
          .resolve("influx.exe"))) {
        targetFolder = CommonUtils.getInstallPath().resolve("influxdb");
      } else {
        targetFolder = downloadAndExtract("https://dl.influxdata.com/influxdb/releases/influxdb2-2.0.6-windows-amd64.zip",
            "influxdb.zip", progressBar, log);
      }
      return targetFolder.resolve("influxdb2-2.0.6-windows-amd64").resolve("influx.exe");
    }
  }

  @Override
  protected void afterDependencyInstalled(@NotNull EntityContext entityContext, Path path) {
    // run db
    runDbIfRequire(entityContext);
    InfluxDBConfigureButtonSetting.configure(entityContext);
  }

  @Override
  public boolean checkWinDependencyInstalled(MachineHardwareRepository repository, @NotNull Path targetPath) {
    return !repository.execute(targetPath + " version").startsWith("Influx CLI");
  }

  @Override
  public @NotNull Class<? extends SettingPluginText> getDependencyPluginSettingClass() {
    return InfluxDBPathSetting.class;
  }

  @Override
  public Class<? extends SettingPluginButton> getInstallButton() {
    return InfluxDBInstallSetting.class;
  }

  /**
   * Start db if need
   */
  public void runDbIfRequire(EntityContext entityContext) {
    if (!entityContext.setting().getValue((InfluxDBRunAtStartupSetting.class))) {
      return;
    }
    String cliPath = entityContext.setting().getValue(InfluxDBPathSetting.class);
    if (cliPath != null) {
      Path installPath = Paths.get(cliPath);
      if (Files.isRegularFile(installPath)) {
        boolean requireInstallDependencies = entityContext.getBean(InfluxDBDependencyExecutableInstaller.class)
            .isRequireInstallDependencies(entityContext, true);
        if (requireInstallDependencies) {
          return;
        }

        Path dbPath = installPath.getParent().resolve(SystemUtils.IS_OS_LINUX ? "influxd" : "influxd.exe");
        entityContext.bgp().builder("InfluxDB run thread").execute(() ->
            entityContext.getBean(MachineHardwareRepository.class).execute(dbPath + " run"));
      }
    }
  }
}
