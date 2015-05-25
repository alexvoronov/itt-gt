package net.gcdc.ittgt.server;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.gcdc.ittgt.client.GroTrClient;
import net.gcdc.ittgt.client.GroTrClient.TcpServerConnection;
import net.gcdc.ittgt.client.GroTrClient.VehicleConnection;
import net.gcdc.ittgt.model.Vehicle;
import net.gcdc.ittgt.model.WorldModel;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroTrServerWithClientTest {
    private final static Logger logger = LoggerFactory.getLogger(GroTrServerWithClientTest.class);

    private static final double oneStepLatChange = 0.01;

    public static class FakeVehicle implements VehicleConnection {

        private Vehicle vehicle;
        public FakeVehicle(Vehicle vehicle) {
            this.vehicle = vehicle;
        }

        @Override public Vehicle receive() throws IOException {
            try { Thread.sleep(3); }  // We don't want to give data to the server too fast.
            catch (InterruptedException e) {
                logger.debug("Interrupted", e);
            } // Ignore interruption.
            vehicle.lat += oneStepLatChange;
            logger.debug("Step for {}: {}", this, vehicle.lat);
            return this.vehicle;
        }

        @Override public void send(WorldModel worldModel) throws IOException {
            vehicle.simulationTimestamp = worldModel.header.simulationTimestamp;
        }
    }


    @Test public void test() throws IOException, InterruptedException {
        final int port = 4100;
        final WorldModel worldModel = new WorldModel();
        worldModel.header = new WorldModel.Header();
        worldModel.header.samplingTimeMs = 10;
        worldModel.header.version = 1;
        worldModel.header.simulationTimestamp = new Date();
        double vehicle1StartLat = 57;
        double vehicle2StartLat = 67;
        Vehicle vehicle1 = new Vehicle();
        vehicle1.id = 1;
        vehicle1.lat = vehicle1StartLat;
        vehicle1.lon = 13;
        Vehicle vehicle2 = new Vehicle();
        vehicle2.id = 2;
        vehicle2.lat = vehicle2StartLat;
        vehicle2.lon = 13.5;
        worldModel.vehicles = new Vehicle[] { vehicle1, vehicle2 };
        final long timeoutMillis = 3 * 1000;

        BasicGroTrServer server = new BasicGroTrServer(worldModel, timeoutMillis);
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(new ClientConnectionsSpawner(port, server));
        Thread.sleep(50);  // Just to let the server start listening on the TCP port.
        InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        TcpServerConnection serverConnection1 = new TcpServerConnection(serverAddress);
        TcpServerConnection serverConnection2 = new TcpServerConnection(serverAddress);
        FakeVehicle fakeVehicleConn1 = new FakeVehicle(vehicle1);
        FakeVehicle fakeVehicleConn2 = new FakeVehicle(vehicle2);
        GroTrClient client1 = new GroTrClient(serverConnection1, fakeVehicleConn1);
        GroTrClient client2 = new GroTrClient(serverConnection2, fakeVehicleConn2);
        client1.start();
        client2.start();

        int minExpectedSteps = 3;

        client1.awaitTermination(200, TimeUnit.MILLISECONDS);
        client2.awaitTermination(200, TimeUnit.MILLISECONDS);
        assertThat("veh1 lat", vehicle1.lat, greaterThan(vehicle1StartLat + minExpectedSteps * oneStepLatChange));
        assertThat("veh2 lat", vehicle2.lat, greaterThan(vehicle2StartLat + minExpectedSteps * oneStepLatChange));
//        assertTrue(vehicle1.lat > vehicle1StartLat + minExpectedSteps * oneStepLatChange);
//        assertTrue(vehicle2.lat > vehicle2StartLat + minExpectedSteps * oneStepLatChange);
    }

}
