package net.gcdc.grotr.ittgt.gpsd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GpsdServer implements AutoCloseable {

    private final Collection<Client>    clients         = new ArrayList<>();
    private final Collection<Future<?>> submitted       = new ArrayList<>();
    private final ExecutorService       executorService = Executors.newCachedThreadPool();

    final static Logger logger = LoggerFactory.getLogger(GpsdServer.class);

    private GpsdServer() {
    }

    public void setLastSeen(GpsData data) {
        for (Client c : clients) {
            c.setLastSeen(data);
        }
    }

    public static GpsdServer start(final int port) {
        final GpsdServer server = new GpsdServer();
        Runnable serverRunnable = new Runnable() {

            @Override
            public void run() {
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    while (true) {
                        final Socket socket = serverSocket.accept();  // accept() blocks!
                        Client client = new Client(socket);
                        logger.info("Got client from {}", socket.getRemoteSocketAddress());
                        Future<?> clientFuture = server.executorService.submit(client);
                        server.clients.add(client);
                        server.submitted.add(clientFuture);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Future<?> serverFuture = server.executorService.submit(serverRunnable);
        server.submitted.add(serverFuture);
        return server;
    }

    @Override
    public void close() throws IOException {
        for (Future<?> f : submitted) {
            f.cancel(true);
        }
        executorService.shutdownNow();

        for (Client client : clients) {
            client.close();
        }
    }

    private static class Client implements Runnable, AutoCloseable {
        private final Socket         socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final Gson           gson           = new GsonBuilder().create();
        private GpsData              lastSeen       = null;
        private boolean              dumpJson       = false;
        private Date                 firstTimestamp = null;

        final Logger logger = LoggerFactory.getLogger(Client.class);

        public Client(final Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }

        public void setLastSeen(GpsData data) {
            logger.info("Received new data");
            lastSeen = data;
            if (firstTimestamp == null) {
                firstTimestamp = data.timestamp();
            }
            if (dumpJson) {
                try {
                    writer.write(gson.toJson(tpvFromGpsData(lastSeen)) + "\n");
                    writer.flush();
                } catch (IOException e) {
                    logger.warn("Could not write TPV reply", e);
                }
            }
        }

        private Devices getDevices() {
            Device device = new Device();
            device.activated = firstTimestamp;
            device.path = "Simuilnk";
            Devices devices = new Devices();
            devices.devices = new Device[] { device };
            return devices;
        }

        private Watch getWatch() {
            Watch watch = new Watch();
            watch.json = dumpJson;
            return watch;
        }

        private TPV tpvFromGpsData(final GpsData data) {
            return TPV.builder()
                    .device("GroTr-Client")
                    .lat(data.lat())
                    .lon(data.lon())
                    .track(data.bearing())
                    .speed(data.speed())
                    .create();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    final String line = reader.readLine();
                    if (line == null) {
                        logger.info("End of stream");
                        break;
                    }
                    logger.info("Received request: {}", line);
                    if (line.startsWith("?POLL;")) {
                        Poll poll = new Poll();
                        if (lastSeen != null) {
                            poll.time = lastSeen.timestamp();
                            poll.tpv = new TPV[] { tpvFromGpsData(lastSeen) };
                        }
                        writer.write(gson.toJson(poll) + "\n");
                    } else if (line.startsWith("?DEVICES;")) {
                        writer.write(gson.toJson(getDevices()) + "\n");
                    } else if (line.startsWith("?WATCH;")) {
                        writer.write(gson.toJson(getDevices()) + "\n");
                        writer.write(gson.toJson(getWatch()) + "\n");
                    } else if (line.startsWith("?WATCH=")) {
                        String request = line.substring(7, line.length() - 1);
                        Watch watchReq = gson.fromJson(request, Watch.class);
                        if (watchReq.json) {
                            dumpJson = true;  // Ignore all other fields.
                        }
                        writer.write(gson.toJson(getDevices()) + "\n");
                        writer.write(gson.toJson(getWatch()) + "\n");
                        writer.flush();
                    } else {
                        logger.warn("Unrecognized request: '{}'. Ignoring", line);
                        Error error = new Error();
                        error.message = "Unrecognized request '" +
                                line.substring(0, line.length() - 1) + "'";  // Drop ending ';'.
                        writer.write(gson.toJson(error) + "\n");
                    }
                }
            } catch (IOException e) {
                logger.warn("Exception in interactive gpsd session, interactive part closes", e);
                // Ignore and terminate this listening client thread.
                // Client will still send event-driven updates if json was enabled in WATCH.
            }
        }

        @Override
        public void close() throws IOException {
            reader.close();
            writer.close();
            socket.close();
        }
    }

}