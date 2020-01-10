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

import li.pitschmann.knx.core.annotations.Nullable;
import li.pitschmann.knx.core.config.ConfigBuilder;
import li.pitschmann.knx.core.config.CoreConfigs;
import li.pitschmann.knx.core.utils.Preconditions;
import li.pitschmann.knx.core.utils.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Main Helper hiding some logic from {@link Main} class as it
 * is not subject to be demonstrated.
 *
 * @author PITSCHR
 */
public final class MainHelper {
    private static final Logger log = LoggerFactory.getLogger(MainHelper.class);

    private MainHelper() {
        throw new AssertionError("Do not touch me!");
    }

    /**
     * Returns the Configuration Builder based on following arguments:
     * <ul>
     * <li>{@code --ip} ... defined endpoint in {@code <address>:<port>} format.
     * If the address is a multicast, then routing will be used, otherwise tunneling (no NAT)</li>
     * <li>{@code --routing} ... if the communication should be over multicast (routing)</li>
     * <li>{@code --nat} ... if the communication should be using Network Address Translation (tunneling)</li>
     * </ul>
     *
     * @param args arguments
     * @return a new instance of {@link ConfigBuilder}
     */
    public static ConfigBuilder parseConfigBuilder(final String[] args) {
        // Argument: Routing enabled?
        final var routingEnabled = existsParameter(args, "--routing");
        log.debug("Routing: {}", routingEnabled);

        // Argument: NAT? (not to be used in routing mode)
        final var natEnabled = existsParameter(args, "--nat");
        log.debug("NAT: {}", natEnabled);

        // Argument: Get KNX Net/IP Address (<address>:<port>)
        final var ipAddress = getParameterValue(args, "--ip", Function.identity(), null);
        log.debug("KNX Net/IP Address: {}", ipAddress);

        if (ipAddress != null) {
            Preconditions.checkState(!routingEnabled, "You cannot use tunneling and routing at same time!");
            // specific endpoint defined
            // decision of routing/tunneling will be done based on ip address
            return ConfigBuilder.create(ipAddress).setting(CoreConfigs.NAT, natEnabled);
        } else if (routingEnabled) {
            Preconditions.checkState(!natEnabled, "NAT is available for tunneling only!");
            // routing
            return ConfigBuilder.routing();
        } else {
            // tunneling (with NAT / without NAT)
            return ConfigBuilder.tunneling(natEnabled);
        }
    }

    /**
     * Returns the value of parameter if supplied
     *
     * @param args           arguments
     * @param parameterNames parameter names, may be comma-separated
     * @param defaultValue   default value in case the parameter could not be found or not parsed correctly
     * @param function       to be used for conversion from String to {@code <T>} value type
     * @param <T>            type of value
     * @return the value of parameter, otherwise {@code defaultValue}
     */
    @Nullable
    public static <T> T getParameterValue(final String[] args,
                                          final String parameterNames,
                                          final Function<String, T> function,
                                          final @Nullable T defaultValue) {
        for (final var parameterName : parameterNames.split(",")) {
            for (var i = 0; i < args.length; i++) {
                if (parameterName.equals(args[i])) {
                    // found - next argument should be the value
                    if ((i + 1) < args.length) {
                        try {
                            return function.apply(args[i + 1]);
                        } catch (final Throwable t) {
                            log.info("Could not parse value '{}'. Default value to be returned: {}", args[i + 1], defaultValue);
                            // could not be parsed
                            return defaultValue;
                        }
                    }
                }
            }
        }
        // not found
        return defaultValue;
    }

    /**
     * Returns the value if parameter exists
     *
     * @param args           arguments
     * @param parameterNames parameter names, may be comma-separated
     * @return {@code true} if parameter was found, otherwise {@code false}
     */
    public static boolean existsParameter(final String[] args,
                                          final String parameterNames) {
        for (final var parameterName : parameterNames.split(",")) {
            if (Arrays.stream(args).anyMatch(parameterName::equals)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the time is overdue
     *
     * @param stopwatch            the current timer
     * @param monitorTimeInSeconds maximum monitor time in seconds
     * @return {@code true} if overdue, otherwise {@code false}
     */
    public static boolean isOverdue(final Stopwatch stopwatch, long monitorTimeInSeconds) {
        return stopwatch.elapsed(TimeUnit.SECONDS) > monitorTimeInSeconds;
    }

    /**
     * Returns given {@code seconds} into a human readable format
     *
     * @param seconds number of seconds to be converted
     * @return human readable time format in 0 days, 0 hours, 0 minutes, 0 seconds
     */
    public static String toHumanTimeFormat(final long seconds) {
        long sec = seconds % 60;
        long minutes = seconds % 3600 / 60;
        long hours = seconds % 86400 / 3600;
        long days = seconds / 86400;

        return String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, sec);
    }
}
