package net.gcdc.ittgt.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
    private final Collection<ClientConnection> clients = new HashSet<>();
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
        final long timeoutMillis = 4 * 1000;
        logger.info("Starting server on port {}, expecting {} clients, with client timeout {} s.",
                port, worldModel.vehicles.length, timeoutMillis/1000);

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
                        logger.info("generating world model");
                        generateNextWorldModel();
                        sendWorldModel();
                    } catch (IOException e) {
                        logger.warn("IO exception in sending to client, ignoring", e);
                    }
                }
            } catch (InterruptedException e) {
                logger.info("Interrupted sender thread", e);
            } catch (Exception e) {
                logger.error("Exception in model distributor", e);
            }
        }
    };

    private void resetVehicleCounter() {
        logger.debug("Start the wait for {} vehicles", allVehicleIds.size());
        counterVehiclesThisStep = new CountDownLatch(allVehicleIds.size());
        idToVehicleThisStep.clear();
    }

    private void awaitAllVehicles() throws InterruptedException {
        counterVehiclesThisStep.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Updates {@link #lastCompleteWorldModel} in-place.
     *
     * TODO: do we need synchronized here?
     */
    private void generateNextWorldModel() {
        lastCompleteWorldModel.header.simulationTimestamp = new Date(lastCompleteWorldModel.header.simulationTimestamp.getTime()
                + lastCompleteWorldModel.header.samplingTimeMs);
        for (int i = 0; i < lastCompleteWorldModel.vehicles.length; i++) {
            int vehicleId = lastCompleteWorldModel.vehicles[i].id;
            if (idToVehicleThisStep.containsKey(vehicleId)) {
                Vehicle updatedVehicle = idToVehicleThisStep.get(vehicleId);
                if (vehicleId != updatedVehicle.id) {
                    logger.error("internal ITT assertion error: vehicle id does not match the key it was stored with");
                }
                lastCompleteWorldModel.vehicles[i] = updatedVehicle;
            }
        }
        logger.debug("lastCompleteWorldModel: {}", lastCompleteWorldModel);
    }

    private void sendWorldModel() throws IOException {
        for (ClientConnection c : clients) {
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

    @Override public void registerAnonymous(ClientConnection clientConnection) {
        if (!clients.contains(clientConnection)) {
            clients.add(clientConnection);
            clientConnection.send(lastCompleteWorldModel);
        }
    }

    /**
     * Check that there is no client with this vehicle-id already.
     */
    @Override public boolean register(int vehicleId, ClientConnection clientConnection) {
        if (!allVehicleIds.contains(vehicleId)) {
            logger.warn("Unknown vehicle id '{}', ignoring", vehicleId);
            return false;
        }
        if (!idToClient.containsKey(vehicleId)) {
            idToClient.put(vehicleId, clientConnection);
            registerAnonymous(clientConnection);  // Just to make sure that it is also there.
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

    @Override public void unregister(ClientConnection clientConnection) {
        for (Entry<Integer, ClientConnection> e: idToClient.entrySet()) {
            if (e.getValue().address().equals(clientConnection.address())) {
                idToClient.remove(e.getKey());
            }
        }
        clients.remove(clientConnection);
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
