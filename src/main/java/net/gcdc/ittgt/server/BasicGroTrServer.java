package net.gcdc.ittgt.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.gcdc.ittgt.model.Vehicle;
import net.gcdc.ittgt.model.WorldModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class BasicGroTrServer implements GroTrServer, AutoCloseable {

    private final long timeoutMillis;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Set<Integer> allVehicleIds = new HashSet<>();
    private final Map<Integer, ClientConnection> idToClient = new HashMap<>();
    private final ConcurrentHashMap<Integer, Vehicle> idToVehicleThisStep = new ConcurrentHashMap<>();
    private CountDownLatch counterVehiclesThisStep;
    private WorldModel lastCompleteWorldModel;
    private Future<?> distributorFuture;

    private final static Logger logger = LoggerFactory.getLogger(BasicGroTrServer.class);
    private final static Gson   gson   = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").create();  // TODO: unify with other GSONS
//    private final static Gson   gson   = new GsonBuilder().create();

    public static void main(String[] args) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
        final int port = Integer.parseInt(args[0]);
        final WorldModel worldModel = gson.fromJson(
                new BufferedReader(new FileReader(args[1])),
                WorldModel.class);
        final long timeoutMillis = 10 * 1000;

        BasicGroTrServer server = new BasicGroTrServer(worldModel, timeoutMillis);
        Executors.newSingleThreadExecutor().submit(new ClientConnectionsSpawner(port, server));
    }

    public BasicGroTrServer(final WorldModel initiallWorld, final long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        this.lastCompleteWorldModel = initiallWorld;
        for (Vehicle v : initiallWorld.vehicles) { allVehicleIds.add(v.id); }
        distributorFuture = executorService.submit(worldModelDistributor);
    }

    private final Runnable worldModelDistributor = new Runnable() {

        @Override public void run() {
            try {
                while (true) {
                    try {
                        resetVehicleCounter();
                        awaitAllVehicles();
                        logger.info("All vehicles received, generating world model");
                        generateNextWorldModel();
                        sendWorldModel();
                    } catch (IOException e) {
                        logger.warn("IO exception in sending to client, ignoring", e);
                    }
                }
            } catch (InterruptedException e) {
                logger.info("Interrupted sender thread", e);
            }
        }
    };

    private void resetVehicleCounter() {
        logger.debug("resetting vehicle counter to {}", allVehicleIds.size());
        counterVehiclesThisStep = new CountDownLatch(allVehicleIds.size());
        idToVehicleThisStep.clear();
    }

    private void awaitAllVehicles() throws InterruptedException {
        counterVehiclesThisStep.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private void generateNextWorldModel() {
        WorldModel model = new WorldModel();
        model.header = lastCompleteWorldModel.header;
        model.header.simulationTimestamp = new Date(model.header.simulationTimestamp.getTime()
                + model.header.samplingTimeMs);
        int numVehicles = idToVehicleThisStep.values().size();
        model.vehicles = idToVehicleThisStep.values().toArray(new Vehicle[numVehicles]);
        lastCompleteWorldModel = model;
    }

    private void sendWorldModel() throws IOException {
        for (int id : idToClient.keySet()) {
            ClientConnection c = idToClient.get(id);
            logger.debug("Sending world model to {}", c);
            c.send(lastCompleteWorldModel);
        }
    }

    @Override public void close() throws IOException {
        if (distributorFuture != null) {
            distributorFuture.cancel(true);
        }
        executorService.shutdownNow();
    }

    @Override public boolean register(int vehicleId, ClientConnection clientConnection) {
        if (!allVehicleIds.contains(vehicleId)) {
            logger.warn("Unknown vehicle id '{}', ignoring", vehicleId);
            return false;
        }
        if (!idToClient.containsKey(vehicleId)) {
            idToClient.put(vehicleId, clientConnection);
            clientConnection.send(lastCompleteWorldModel);
            return true;
        }
        if (idToClient.get(vehicleId).address().equals(clientConnection.address())) {
            logger.warn("Duplicate registeration for vehicle '{}' from {}, ignoring", vehicleId,
                    clientConnection.address());
            return true;
        } else {
            logger.warn(
                    "Attempt to register vehicle '{}' from {}, but vehicle already reistered to {}",
                    vehicleId, clientConnection.address(), idToClient.get(vehicleId).address());
            return false;
        }
    }

    @Override public void updateVehicleState(Vehicle vehicle, ClientConnection clientConnection) {

        // Check if address is correct.
        if (!idToClient.containsKey(vehicle.id)) {
            logger.warn("Unregistered vehicle id {} from {}", vehicle.id, clientConnection.address());
            return;
        }
        logger.trace("id to clients {}, client conn {}", idToClient, clientConnection);
        logger.trace("client conn from id {}, addr from map {}, addr from call {}", idToClient.get(vehicle.id), idToClient.get(vehicle.id).address(), clientConnection.address());
        if (!idToClient.get(vehicle.id).address().equals(clientConnection.address())) {
            logger.warn("Bad address for vehicle id {}: expected {}, got {}", vehicle.id,
                    idToClient.get(vehicle.id).address(), clientConnection.address());
            return;
        }

        // TODO: check if timestamp is correct.

        // Add vehicle.
        boolean seenBefore = idToVehicleThisStep.containsKey(vehicle.id);
        idToVehicleThisStep.put(vehicle.id, vehicle);
        // Make sure that counterVehiclesThisStep was initialized.
        while (counterVehiclesThisStep == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for counterVehiclesThisStep to become non-null");
            }
        }
        if (!seenBefore) {
            counterVehiclesThisStep.countDown();
        } else {
            logger.debug("Duplicate vehicle data received for this timestep from vehicle {} at {}", vehicle.id, clientConnection.address());
        }
    }

}
