package net.gcdc.ittgt.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.gcdc.ittgt.model.Vehicle;
import net.gcdc.ittgt.model.WorldModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class GroTrClient {

    private final static Logger logger = LoggerFactory.getLogger(GroTrClient.class);
    private final VehicleConnection vehicleConnection;
    private final ServerConnection serverConnection;
    private final static ExecutorService executor = Executors.newCachedThreadPool();


    public static interface VehicleConnection {
        public Vehicle receive() throws IOException;
        public void send(WorldModel worldModel) throws IOException;
    }

    public static interface ServerConnection {
        public void send(Vehicle vehicle) throws IOException;
        public WorldModel receive() throws IOException;
    }



    public static class TcpServerConnection implements ServerConnection, AutoCloseable {

        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                .create();  // TODO: unify with other gsons!
        //private final Gson gson = new GsonBuilder().create();

        public TcpServerConnection(final InetSocketAddress grotrServerAddress) throws IOException {
            socket = new Socket(grotrServerAddress.getAddress(), grotrServerAddress.getPort());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }

        @Override public void send(final Vehicle vehicle) {
            String json = null;
            try {
                json = gson.toJson(vehicle);

            } catch (Exception e) {  //TODO: which exception toJson throws?
                logger.warn("can't write json", e);
            }
            if (json != null) {
            try {
                writer.write(json);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                logger.warn("can't send json", e);
            }
            }
        }

        @Override public WorldModel receive() throws IOException {
            final String line = reader.readLine();
            try {
                final WorldModel world = gson.fromJson(line, WorldModel.class);
                return world;
            } catch (com.google.gson.JsonSyntaxException e) {
                logger.warn("Can't parse json: {}", line, e);
                throw e;
            }
        }

        @Override public void close() throws IOException {
            reader.close();
            writer.close();
            socket.close();
        }
    }

    public static class UdpSimulinkConnection implements VehicleConnection, AutoCloseable {
        private final DatagramSocket socketFromSimulink;
        private final byte[] bufferFromSimulink = new byte[65535];
        private final DatagramPacket packetFromSimulink = new DatagramPacket(bufferFromSimulink,
                bufferFromSimulink.length);

        private final DatagramSocket socketToSimulink;
        private final byte[] bufferToSimulink = new byte[65535];
        private final DatagramPacket packetToSimulink = new DatagramPacket(bufferToSimulink,
                bufferToSimulink.length);

        public UdpSimulinkConnection(
                final InetSocketAddress remoteSimulinkAddress,
                final int localPortForSimulink) throws SocketException {
            socketFromSimulink = new DatagramSocket(localPortForSimulink);
            socketToSimulink = new DatagramSocket();
            packetToSimulink.setSocketAddress(remoteSimulinkAddress);
        }

        @Override public Vehicle receive() throws IOException {
            socketFromSimulink.receive(packetFromSimulink);
            byte[] data = Arrays.copyOf(packetFromSimulink.getData(),
                    packetFromSimulink.getLength());
            SimulinkGt simulinkGt = SimulinkParser.parse(data, SimulinkGt.class);
            logger.debug("Got from simulink: {}", simulinkGt);
            Vehicle vehicle = vehicleFromSimulinkGt(simulinkGt);
            return vehicle;
        }

        private Vehicle vehicleFromSimulinkGt(SimulinkGt simulinkGt) {
            Vehicle vehicle = new Vehicle();
            vehicle.id = (int) simulinkGt.vehicleId;
            vehicle.lat = simulinkGt.x;
            vehicle.lon = simulinkGt.y;
            vehicle.speedMetersPerSecond = simulinkGt.v;
            return vehicle;
        }

        @Override public void send(WorldModel worldModel) throws IOException {
            // TODO: put worldModel into byte array 'bufferToSimulink'.
            // socketToSimulink.send(packetToSimulink);
        }

        @Override public void close() {
            socketFromSimulink.close();
            socketToSimulink.close();
        }

    }

    public static class SocketAddressFromString {
        private final InetSocketAddress address;

        public InetSocketAddress asInetSocketAddress() {
            return address;
        }

        @SuppressWarnings("unused")// JewelCLI uses this constructor through reflection.
        public SocketAddressFromString(final String addressStr) {
            String[] hostAndPort = addressStr.split(":");
            if (hostAndPort.length != 2) { throw new ArgumentValidationException(
                    "Expected host:port, got " + addressStr); }
            this.address = new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
        }
    }

    private static interface CliOptions {
        @Option SocketAddressFromString getRemoteSimulinkAddress();
        @Option SocketAddressFromString getGrotrServerAddress();
        @Option int getLocalPortForSimulink();
        @Option(helpRequest = true) boolean getHelp();
    }

    public static void main(final String[] args)
            throws InterruptedException, ArgumentValidationException {
        CliOptions opts = CliFactory.parseArguments(CliOptions.class, args);
        try (UdpSimulinkConnection vehicleConnection = new UdpSimulinkConnection(
                    opts.getRemoteSimulinkAddress().asInetSocketAddress(),
                    opts.getLocalPortForSimulink());
                TcpServerConnection serverConnection = new TcpServerConnection(
                    opts.getGrotrServerAddress().asInetSocketAddress());
            ) {
            GroTrClient client = new GroTrClient(serverConnection, vehicleConnection);
            client.start();
        } catch (IOException e) {
            logger.error("IO exception in client, terminating", e);
        }
    }

    public GroTrClient(ServerConnection serverConnection, VehicleConnection vehicleConnection) {
        this.vehicleConnection = vehicleConnection;
        this.serverConnection = serverConnection;
    }

    public void start() {
        executor.submit(this.senderThread);
        executor.submit(this.receiverThread);
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        executor.awaitTermination(timeout, unit);
    }


    private final Runnable senderThread = new Runnable() {
        @Override public void run() {
            try {
                while (true) {
                    logger.debug("waiting for vehicle from client");
                    Vehicle vehicle = vehicleConnection.receive();
                    logger.debug("got vehicle from client");
                    serverConnection.send(vehicle);
                    logger.debug("vehicle sent to server");
                }
            } catch(Exception e) {
                logger.error("Exception in ITT GT server thread", e);
            }
        }
    };

    private final Runnable receiverThread = new Runnable() {
        @Override public void run() {
            try {
                while (true) {
                    logger.debug("waiting for world from server");
                    WorldModel worldModel = serverConnection.receive();
                    logger.debug("got world");
                    vehicleConnection.send(worldModel);
                    logger.debug("World model sent to client");
                }
            } catch(Exception e) {
                logger.error("Exception in ITT GT client thread", e);
            }
        }
    };
}
