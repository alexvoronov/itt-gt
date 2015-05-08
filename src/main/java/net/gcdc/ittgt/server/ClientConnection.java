package net.gcdc.ittgt.server;

import net.gcdc.ittgt.model.WorldModel;

/** Client interface that ITT GT Server uses towards all its clients.
 *
 * This interface is just an abstraction that ITT GT Server uses, it has nothing to do with how the
 * client itself is implemented.
 * */
public interface ClientConnection {

    void send(WorldModel model);

    String address();
}
