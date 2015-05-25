package net.gcdc.ittgt.server;

import net.gcdc.ittgt.model.Vehicle;

public interface GroTrServer {

    void updateVehicleState(Vehicle vehicle, ClientConnection clientConnection);

    boolean register(int vehicleId, ClientConnection clientConnection);

    void unregister(ClientConnection clientConnection);

}
