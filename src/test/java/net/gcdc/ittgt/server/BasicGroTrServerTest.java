package net.gcdc.ittgt.server;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import net.gcdc.ittgt.model.Vehicle;
import net.gcdc.ittgt.model.WorldModel;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

public class BasicGroTrServerTest {

    class IsAnyWorldModel extends ArgumentMatcher<WorldModel> {
        @Override public boolean matches(Object that) {
            return (that != null) && (that instanceof WorldModel);
        }
    }

    @Test public void test() throws InterruptedException {
        final int port = 10000;
        WorldModel model = new WorldModel();
        model.header = new WorldModel.Header();
        model.header.samplingTimeMs = 10;
        model.header.version = 1;
        model.header.simulationTimestamp = new Date();
        Vehicle vehicle1 = new Vehicle();
        vehicle1.id = 1;
        vehicle1.lat = 57;
        vehicle1.lon = 13;
        Vehicle vehicle2 = new Vehicle();
        vehicle2.id = 2;
        vehicle2.lat = 57.5;
        vehicle2.lon = 13.5;
        model.vehicles = new Vehicle[] { vehicle1, vehicle2 };
        GroTrServer server = new BasicGroTrServer(model, 1000 * 10);
        // Executors.newSingleThreadExecutor().submit(new SocketClientSpawner(port, server));
        ClientConnection connection1 = Mockito.mock(ClientConnection.class);
        ClientConnection connection2 = Mockito.mock(ClientConnection.class);
        when(connection1.address()).thenReturn("1");
        when(connection2.address()).thenReturn("2");

        // Registers sends world model
        server.register(vehicle1.id, connection1);
        server.register(vehicle2.id, connection2);
        verify(connection1).send(argThat(new IsAnyWorldModel()));
        verify(connection2).send(argThat(new IsAnyWorldModel()));

        server.updateVehicleState(vehicle1, connection1);
        server.updateVehicleState(vehicle2, connection2);

        Thread.sleep(10);
        verify(connection1, times(2)).send(argThat(new IsAnyWorldModel()));
        verify(connection2, times(2)).send(argThat(new IsAnyWorldModel()));
    }

    @Test public void test2() throws InterruptedException {
        final int port = 10000;
        WorldModel model = new WorldModel();
        model.header = new WorldModel.Header();
        model.header.samplingTimeMs = 10;
        model.header.version = 1;
        model.header.simulationTimestamp = new Date();
        Vehicle vehicle1 = new Vehicle();
        vehicle1.id = 1;
        vehicle1.lat = 57;
        vehicle1.lon = 13;
        Vehicle vehicle2 = new Vehicle();
        vehicle2.id = 2;
        vehicle2.lat = 57.5;
        vehicle2.lon = 13.5;
        model.vehicles = new Vehicle[] { vehicle1, vehicle2 };
        GroTrServer server = new BasicGroTrServer(model, 1000 * 10);
        // Executors.newSingleThreadExecutor().submit(new SocketClientSpawner(port, server));
        ClientConnection connection1 = Mockito.mock(ClientConnection.class);
        ClientConnection connection2 = Mockito.mock(ClientConnection.class);
        when(connection1.address()).thenReturn("1");
        when(connection2.address()).thenReturn("2");

        // Registers sends world model
        server.register(vehicle1.id, connection1);
        server.register(vehicle2.id, connection2);
        verify(connection1, times(1)).send(argThat(new IsAnyWorldModel()));
        verify(connection2, times(1)).send(argThat(new IsAnyWorldModel()));

        // No sending before received from all
        server.updateVehicleState(vehicle1, connection1);
        Thread.sleep(10);
        verify(connection1, times(1)).send(argThat(new IsAnyWorldModel()));
        verify(connection2, times(1)).send(argThat(new IsAnyWorldModel()));
        server.updateVehicleState(vehicle2, connection2);

        // Send to all after received from all
        Thread.sleep(10);  // Sending thread is separate
        verify(connection1, times(2)).send(argThat(new IsAnyWorldModel()));
        verify(connection2, times(2)).send(argThat(new IsAnyWorldModel()));
    }

    @Test public void testTimeout() throws InterruptedException {
        final int port = 10000;
        WorldModel model = new WorldModel();
        model.header = new WorldModel.Header();
        model.header.samplingTimeMs = 10;
        model.header.version = 1;
        model.header.simulationTimestamp = new Date();
        Vehicle vehicle1 = new Vehicle();
        vehicle1.id = 1;
        vehicle1.lat = 57;
        vehicle1.lon = 13;
        Vehicle vehicle2 = new Vehicle();
        vehicle2.id = 2;
        vehicle2.lat = 57.5;
        vehicle2.lon = 13.5;
        model.vehicles = new Vehicle[] { vehicle1, vehicle2 };
        final int timeoutMillis = 300;
        GroTrServer server = new BasicGroTrServer(model, timeoutMillis);
        // Executors.newSingleThreadExecutor().submit(new SocketClientSpawner(port, server));
        ClientConnection connection1 = Mockito.mock(ClientConnection.class);
        ClientConnection connection2 = Mockito.mock(ClientConnection.class);
        when(connection1.address()).thenReturn("1");
        when(connection2.address()).thenReturn("2");

        // Registers sends world model
        server.register(vehicle1.id, connection1);
        server.register(vehicle2.id, connection2);
        verify(connection1, times(1)).send(argThat(new IsAnyWorldModel()));
        verify(connection2, times(1)).send(argThat(new IsAnyWorldModel()));

        // No sending before received from all
        server.updateVehicleState(vehicle1, connection1);
        Thread.sleep(50);
        verify(connection1, times(1)).send(argThat(new IsAnyWorldModel()));
        verify(connection2, times(1)).send(argThat(new IsAnyWorldModel()));

        // Send to all after a timeout
        Thread.sleep(timeoutMillis);
        verify(connection1, times(2)).send(argThat(new IsAnyWorldModel()));
        verify(connection2, times(2)).send(argThat(new IsAnyWorldModel()));
    }
}
