/*
 * KNX Link - A library for KNX Net/IP communication
 * Copyright (C) 2020 Pitschmann Christoph
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package li.pitschmann.knx.examples.tty;

import li.pitschmann.knx.core.communication.DefaultKnxClient;
import li.pitschmann.knx.core.config.CoreConfigs;
import li.pitschmann.knx.core.plugin.audit.FileAuditPlugin;
import li.pitschmann.knx.core.plugin.statistic.FileStatisticFormat;
import li.pitschmann.knx.core.plugin.statistic.FileStatisticPlugin;
import li.pitschmann.knx.core.utils.Sleeper;
import li.pitschmann.knx.core.utils.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static li.pitschmann.knx.examples.tty.MainHelper.existsParameter;
import static li.pitschmann.knx.examples.tty.MainHelper.getParameterValue;
import static li.pitschmann.knx.examples.tty.MainHelper.isOverdue;
import static li.pitschmann.knx.examples.tty.MainHelper.parseConfigBuilder;
import static li.pitschmann.knx.examples.tty.MainHelper.toHumanTimeFormat;

/**
 * Demo class how to monitor the KNX traffic with support of plug-ins:
 * <ul>
 *     <li>{@link FileAuditPlugin} ... auditing the KNX traffic</li>
 *     <li>{@link FileStatisticPlugin} ... writing KNX statistic regularly</li>
 *     <li>{@link MonitorPlugin} ... print out KNX traffic to terminal</li>
 * </ul>
 *
 * @author PITSCHR
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        // ----------------------------------------------------------------------
        // Examples
        // (just uncomment the line if you want to test specific scenario)
        // ----------------------------------------------------------------------
        // * Tunneling (using Discovery):
        // arguments = new String[0];

        // * Tunneling (using Discovery + NAT):
        // arguments = new String[]{"--nat"};

        // * Tunneling (using Endpoint):
        // arguments = new String[]{"--ip", "192.168.1.16"};

        // * Tunneling (using Endpoint + NAT):
        // arguments = new String[]{"--ip", "192.168.1.16", "--nat"}

        // * Routing:
        // arguments = new String[]{"--routing"}

        log.debug("Final Arguments: {}", Arrays.toString(args));

        // Get Monitor Time in Seconds (example: 100 seconds)
        // -t 100
        // --time 100
        final var monitorTime = getParameterValue(args, "-t,--time", Long::parseLong, 3600L);
        log.debug("Monitor Time: {}", toHumanTimeFormat(monitorTime));

        // Get XML Project Path
        // -p ~/my-project.knxproj
        // --project ~/my-project.knxproj
        final var projectPath = getParameterValue(args, "-p,--project", Paths::get, null);
        log.debug("KNX Project Path: {}", Objects.requireNonNullElse(projectPath, "<empty>"));

        // If fake data should be generated for demo purposes (checks only if argument is present)
        // -g
        // --generate-fake-data
        final var generateFakeData = existsParameter(args, "-g,--generate-fake-data");
        log.debug("Generate Fake Data?: {}", generateFakeData);

        // Create Config
        final var config = parseConfigBuilder(args)
                // set the path of KNX project file
                .setting(CoreConfigs.PROJECT_PATH, projectPath)
                // register plugins
                .plugin(FileAuditPlugin.class) //
                .plugin(MonitorPlugin.class) //
                .plugin(FileStatisticPlugin.class)
                // hardcoded ports -> useful for docker
                .setting(CoreConfigs.Description.PORT, 40001) //
                .setting(CoreConfigs.Control.PORT, 40002) //
                .setting(CoreConfigs.Data.PORT, 40003)
                // print out the statistic in TEXT (human-friendly)
                .setting(FileStatisticPlugin.FORMAT, FileStatisticFormat.TEXT)
                // defined if the fake data of MonitorPlugin should be generated
                .setting(MonitorPlugin.GENERATE_FAKE_DATA, generateFakeData)
                .setting(CoreConfigs.Search.REQUEST_TIMEOUT, 1000L)
                // build an immutable config
                .build();

        final var sw = Stopwatch.createStarted();
        final var maxAttempts = 1;
        var attempts = 0;
        try {
            log.debug("===================================================================================");
            log.debug("START MONITORING for {}", toHumanTimeFormat(monitorTime));
            log.debug("===================================================================================");

            // loop in case the KNX client loses the connection for some reasons (e.g. power outage?, connection/firewall issue)
            do {

                // create connection and keep alive until monitor time is not overdue
                try (final var client = DefaultKnxClient.createStarted(config)) {
                    while (client.isRunning() && !isOverdue(sw, monitorTime)) {
                        Sleeper.seconds(1);
                    }
                } catch (final Exception ex) {
                    log.error("Throwable got: {}", ex.getMessage(), ex);

                    // add small delay in re-connect in case of an issue
                    if (++attempts < maxAttempts) {
                        log.warn("Re-Connecting ...");
                        Sleeper.seconds(5);
                    }
                }

                // quit this loop if max attempts exceeded or if time is overdue
            } while (attempts < maxAttempts && !isOverdue(sw, monitorTime));
        } finally {
            log.debug("===================================================================================");
            log.debug("STOP MONITORING after {} attempts: {}", attempts, toHumanTimeFormat(sw.elapsed(TimeUnit.SECONDS)));
            log.debug("===================================================================================");
        }
    }

}
