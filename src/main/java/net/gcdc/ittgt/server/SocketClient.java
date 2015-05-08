package net.gcdc.ittgt.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import net.gcdc.ittgt.model.Vehicle;
import net.gcdc.ittgt.model.WorldModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** ITT GT Server abstraction that represents clients connecting through a TCP socket and
 * encodes Ground Truth Data (Vehicle and WorldModel) using JSON. */
class SocketClient implements Runnable, AutoCloseable, ClientConnection {
    @Override public String toString() {
        return String.format("ClientConnection(%s)", address());
    }

    private final GroTrServer    server;
    private final Socket         socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final Logger         logger = LoggerFactory.getLogger(SocketClient.class);
    private final Gson           gson   = new GsonBuilder().create();

    public SocketClient(Socket socket, GroTrServer server) throws IOException {
        this.server = server;
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @Override public String address() {
        return socket.getRemoteSocketAddress().toString();
    }

    @Override public void send(WorldModel model) {
        String json = gson.toJson(model);
        try {
            writer.write(json);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            logger.warn("Failed to write to socket while sending the world model, ignoring", e);
        }
    }

    @Override public void run() {
        try {
            boolean registered = false;
            while (true) {
                final String line = reader.readLine();
                if (line == null) {
                    logger.info("End of stream from client");
                    this.close();
                    return;
                }
                final Vehicle vehicle = gson.fromJson(line, Vehicle.class);
                logger.info("Received vehicle: {}", vehicle);
                if (registered) {
                    this.server.updateVehicleState(vehicle, this);
                } else {
                    registered = server.register(vehicle.id, this);  // Sends current world model too.
                }
            }
        } catch (IOException e) {
            logger.warn("Exception in client session, client seesion closing", e);
        }
    }

    @Override public void close() throws IOException {
        reader.close();
        writer.close();
        socket.close();
    }

}