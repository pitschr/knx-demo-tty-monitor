package li.pitschmann.knx.examples.tty;

import li.pitschmann.knx.core.CEMIAware;
import li.pitschmann.knx.core.address.GroupAddress;
import li.pitschmann.knx.core.address.IndividualAddress;
import li.pitschmann.knx.core.annotations.Nullable;
import li.pitschmann.knx.core.body.Body;
import li.pitschmann.knx.core.body.RoutingIndicationBody;
import li.pitschmann.knx.core.body.TunnelingRequestBody;
import li.pitschmann.knx.core.cemi.APCI;
import li.pitschmann.knx.core.cemi.AdditionalInfo;
import li.pitschmann.knx.core.cemi.CEMI;
import li.pitschmann.knx.core.cemi.ControlByte1;
import li.pitschmann.knx.core.cemi.ControlByte2;
import li.pitschmann.knx.core.cemi.MessageCode;
import li.pitschmann.knx.core.cemi.TPCI;
import li.pitschmann.knx.core.communication.KnxClient;
import li.pitschmann.knx.core.datapoint.DPT8;
import li.pitschmann.knx.core.datapoint.DataPointRegistry;
import li.pitschmann.knx.core.datapoint.DataPointType;
import li.pitschmann.knx.core.knxproj.XmlGroupAddress;
import li.pitschmann.knx.core.knxproj.XmlProject;
import li.pitschmann.knx.core.plugin.BooleanConfigValue;
import li.pitschmann.knx.core.plugin.ExtensionPlugin;
import li.pitschmann.knx.core.plugin.IntegerConfigValue;
import li.pitschmann.knx.core.plugin.ObserverPlugin;
import li.pitschmann.knx.core.utils.ByteFormatter;
import li.pitschmann.knx.core.utils.Sleeper;
import li.pitschmann.knx.core.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TTY Monitor Plugin
 * <p>
 * This plugin is designed to for monitoring
 */
public final class MonitorPlugin implements ObserverPlugin, ExtensionPlugin {
    static final BooleanConfigValue GENERATE_FAKE_DATA = new BooleanConfigValue("generate-fake-data", () -> Boolean.FALSE);
    static final IntegerConfigValue GENERATE_FAKE_DATA_INTERVAL = new IntegerConfigValue("generate-interval", () -> 1000, null);
    private static final Logger log = LoggerFactory.getLogger(MonitorPlugin.class);
    private static final int DEFAULT_SIZE_COLUMN = 80;
    private static final int DEFAULT_SIZE_LINES = 20;
    private static final String DEFAULT_TABLE_HEADER_FOOTER_COLOR = "\033[1;32m";
    private static final String DEFAULT_TABLE_BODY_COLOR = "\033[0;32m";
    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);

    /**
     * The output stream that should be used for printing out to the terminal
     */
    private final PrintStream out;
    /**
     * The number of columns (=width) of terminal
     */
    private final int columns;
    /**
     * The number of lines (=height) of terminal
     */
    private final int lines;
    private final AtomicBoolean emptyTable = new AtomicBoolean(true);
    private final AtomicInteger numberOfIncomingBodies = new AtomicInteger();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    /**
     * <p>If the fake data should be generated. See: {@link FakeDataRunnable}</p>
     * <p>{@code false} = disabled, no fake data will be generated</p>
     * <p>{@code true} = enabled, fake data will be generated and printed out to terminal</p>
     */
    private boolean generateFakeData;
    /**
     * Defines the interval how frequently the fake data should be generated (in milliseconds).
     */
    private int generateFakeDataInterval;
    private KnxClient knxClient;
    private XmlProject xmlProject;

    public MonitorPlugin() {
        this.out = System.out;
        this.columns = getTerminalColumns();
        this.lines = getTerminalLines();

        log.info("Terminal initialized with: out={}, columns={}, lines={}", this.out, this.columns, this.lines);
    }

    /**
     * Returns the column size of terminal, falls back to {@link #DEFAULT_SIZE_COLUMN} if it could not
     * obtained for some reasons.
     *
     * @return width of terminal in number of columns
     */
    private static int getTerminalColumns() {
        final var strColumns = getTerminalOutput("tput cols");
        return strColumns == null ? DEFAULT_SIZE_COLUMN : Integer.parseInt(strColumns);
    }

    /**
     * Returns the lines size of terminal, falls back to {@link #DEFAULT_SIZE_LINES} if it could not
     * obtained for some reasons.
     *
     * @return height of terminal in number of lines
     */
    private static int getTerminalLines() {
        final var strLines = getTerminalOutput("tput lines");
        return strLines == null ? DEFAULT_SIZE_LINES : Integer.parseInt(strLines);
    }

    /**
     * Internal helper to get the terminal output.
     * <p>
     * See: {@link #getTerminalColumns()} and {@link #getTerminalLines()}
     *
     * @param command command to be executed
     * @return output from terminal, {@code null} if something went wrong
     */
    @Nullable
    private static String getTerminalOutput(final String command) {
        final var pb = new ProcessBuilder().command("sh", "-c", command + " 2> /dev/tty");
        try {
            final var process = pb.start();
            try (final var in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = in.readLine();
                if (line != null) {
                    return line;
                }
            }
        } catch (final IOException ioe) {
            log.error("I/O Exception during terminal output: {}", command, ioe);
        }
        return null;
    }

    private String getHeader() {
        return "  #        | Date / Time         | Source    | Target    | DPT      | Value";
    }

    private String getHeaderSeparator() {
        return "-----------+---------------------+-----------+-----------+----------+" + "-".repeat(columns - 70);
    }

    private String getEmptyLine() {
        return "           |                     |           |           |          |";
    }

    @Override
    public void onInitialization(final KnxClient knxClient) {
        this.knxClient = knxClient;
        log.debug("KNX Client: {}", knxClient);

        this.xmlProject = knxClient.getConfig().getProject();
        log.debug("KNXPROJ File: {}", xmlProject);

        // get config if fake data should be generated (incl. interval)
        this.generateFakeData = knxClient.getConfig(GENERATE_FAKE_DATA);
        this.generateFakeDataInterval = knxClient.getConfig(GENERATE_FAKE_DATA_INTERVAL);
        log.debug("Generate Fake Data: {} (interval: {})", generateFakeData, generateFakeDataInterval);
    }

    @Override
    public void onStart() {
        // creates the screen
        printInitialScreen();

        // Execute the time
        executorService.execute(new TimeRunnable());

        // If enabled: Invoke the Fake Data Generator
        if (generateFakeData) {
            executorService.execute(new FakeDataRunnable());
        }
    }

    @Override
    public void onShutdown() {
        executorService.shutdownNow();

        final var sb = new StringBuilder(25);
        // reset scroll region
        sb.append("\033[r");
        // go to top+left position
        sb.append("\033[0;0H");
        // clear screen
        sb.append("\033[2J");
        out.println(sb.toString());
    }

    @Override
    public void onIncomingBody(final Body item) {
        if (item instanceof CEMIAware) {
            printLineInTable(((CEMIAware)item).getCEMI());
        }
    }

    @Override
    public void onOutgoingBody(final Body item) {
        // NO-OP
    }

    @Override
    public void onError(final Throwable throwable) {
        printToTerminal(String.format("[ ERROR ] %s", throwable.getMessage()), "\033[0;31m");
    }

    /**
     * Prints the initial screen, containing header, footer and empty table
     */
    private void printInitialScreen() {
        final var sb = new StringBuilder(200);

        // reset screen region
        sb.append("\033[r");

        // go to top+left position
        sb.append("\033[0;0H");

        // clear entire screen
        sb.append("\033[2J");

        // Headline
        // --------
        sb.append(String.format("KNX MONITOR (%s x %s, Routing: %s, NAT: %s)",
                columns,
                lines,
                knxClient.getConfig().isRoutingEnabled(),
                knxClient.getConfig().isNatEnabled()))
                .append(System.lineSeparator());

        // additional space between headline and table
        sb.append(System.lineSeparator());

        // Table Header and Separator
        // --------------------------
        sb.append(DEFAULT_TABLE_HEADER_FOOTER_COLOR)
                .append(getHeader())
                .append(System.lineSeparator())
                .append(getHeaderSeparator())
                .append(System.lineSeparator());

        // Table Body
        // ----------
        // number of console lines - 4 top lines - 3 bottom lines
        for (int i = 0; i < lines - 4 - 3; i++) {
            sb.append(DEFAULT_TABLE_BODY_COLOR)
                    .append(getEmptyLine())
                    .append(System.lineSeparator());
        }

        // Table Separator (footer)
        // ------------------------
        sb.append(DEFAULT_TABLE_HEADER_FOOTER_COLOR)
                .append(getHeaderSeparator())
                .append(System.lineSeparator());

        // Footline
        // --------
        sb.append("\033[0m")
                .append("Press CTRL+C to quit")
                .append(System.lineSeparator());

        // Scroll Region (Table body)
        // --------------------------
        // start region at line: 5 (4 header lines + 1)
        // end region at line: (console lines - 3)
        sb.append("\033[5;").append(lines - 3).append('r');

        // go to first line of table body
        sb.append("\033[5;0H");

        // save cursor position
        sb.append("\0337");

        // print
        out.print(sb.toString());
    }

    /**
     * Print line in table
     *
     * @param cemi the {@link CEMI} instance that should be printed to table
     */
    private void printLineInTable(final CEMI cemi) {
        try {
            final var sb = new StringBuilder();
            sb.append(String.format("%10s", numberOfIncomingBodies.incrementAndGet()))
                    .append(" | ")
                    .append(String.format("%19s", DATE_TIME_FORMATTER.format(LocalDateTime.now())))
                    .append(" | ");

            final var sourceAddress = cemi.getSourceAddress();
            final var destinationAddress = cemi.getDestinationAddress();

            // source address (always individual)
            sb.append(String.format("%9s", sourceAddress.getAddress()))
                    .append(" | ");

            // destination address in proper style
            if (xmlProject != null && destinationAddress instanceof GroupAddress) {
                sb.append(String.format("%9s", xmlProject.getGroupAddressStyle().toString((GroupAddress) destinationAddress)));
            } else {
                sb.append(String.format("%9s", ByteFormatter.formatHexAsString(destinationAddress.toByteArray())));
            }

            // get data point type
            XmlGroupAddress xmlGroupAddress;
            DataPointType dpt = null;
            if (xmlProject != null && destinationAddress instanceof GroupAddress) {
                xmlGroupAddress = xmlProject.getGroupAddress((GroupAddress) destinationAddress);
                if (xmlGroupAddress != null) {
                    final var dptString = xmlGroupAddress.getDataPointType();
                    if (!Strings.isNullOrEmpty(dptString)) {
                        dpt = DataPointRegistry.getDataPointType(dptString);
                    }
                }
            }

            final var dptString = (dpt == null) ? "n/a" : dpt.getId();
            sb.append(" | ")
                    .append(String.format("%8s", dptString))
                    .append(" | ");

            // value of data
            final String dptValueString;
            if (dpt != null) {
                final var dptValue = dpt.of(cemi.getData());
                dptValueString = String.format("%s %s", dptValue.toText(), dpt.getUnit());
            } else {
                dptValueString = ByteFormatter.formatHexAsString(cemi.getData());
            }
            sb.append(dptValueString);

            printToTerminal(sb.toString());
        } catch (final Throwable t) {
            log.error("Error during print to terminal", t);
        }
    }

    /**
     * Prints the line to terminal table with default {@code escapeCode} taken
     * from {@link #DEFAULT_TABLE_BODY_COLOR}
     *
     * @param str the string to be printed to terminal
     */
    private void printToTerminal(final String str) {
        printToTerminal(str, DEFAULT_TABLE_BODY_COLOR);
    }

    /**
     * Prints the line to terminal table with specific {@code escapeCode}
     *
     * @param str        the string to be printed to terminal
     * @param escapeCode the escape code to be invoked before printing to terminal (e.g. set color)
     */
    private synchronized void printToTerminal(final String str, final String escapeCode) {
        out.print(String.format("\0338\033[K%s%s%s\033[0m\0337", escapeCode, emptyTable.getAndSet(false) ? "" : System.lineSeparator(), str));
    }

    /**
     * Runnable for updating the time
     */
    private class TimeRunnable implements Runnable {
        @Override
        public void run() {
            final var timePosition = "\033[1;70H\033[K\033[" + (columns - TIME_PATTERN.length()) + "G";
            do {
                final var nowStr = DATE_TIME_FORMATTER.format(LocalDateTime.now());
                out.print(String.format("\0338%s%s\0338", timePosition, nowStr));
            } while (Sleeper.seconds(1));
        }
    }

    /**
     * Runnable for fake data (helpful for debugging purposes)
     */
    private class FakeDataRunnable implements Runnable {
        private final AtomicInteger dummyIncrement = new AtomicInteger();

        @Override
        public void run() {
            do {
                final var inc = dummyIncrement.getAndIncrement() % 256;

                final var sourceAddress = IndividualAddress.of(15, 15, inc);
                final var destinationAddress = GroupAddress.of(31, 7, inc);
                final CEMI cemi = CEMI.of(
                        MessageCode.L_DATA_IND,
                        AdditionalInfo.empty(),
                        ControlByte1.useDefault(),
                        ControlByte2.of(destinationAddress),
                        sourceAddress,
                        destinationAddress,
                        TPCI.UNNUMBERED_PACKAGE,
                        0,
                        APCI.GROUP_VALUE_WRITE,
                        DPT8.VALUE_2_OCTET_COUNT.of(inc * 127)
                );
                printLineInTable(cemi);
            } while (Sleeper.milliseconds(generateFakeDataInterval));
        }
    }
}
