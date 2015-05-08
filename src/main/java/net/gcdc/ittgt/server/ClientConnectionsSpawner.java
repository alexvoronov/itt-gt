package net.gcdc.ittgt.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientConnectionsSpawner implements Runnable, AutoCloseable {
    private final int                      port;
    private final GroTrServer              server;
    private final Collection<SocketClient> socketClients   = new ArrayList<>();
    private final Collection<Future<?>>    submitted       = new ArrayList<>();
    private final ExecutorService          executorService = Executors.newCachedThreadPool();
    private final static Logger            logger          = LoggerFactory
                                                                   .getLogger(ClientConnectionsSpawner.class);

    public ClientConnectionsSpawner(int port, GroTrServer server) {
        this.port = port;
        this.server = server;
    }

    @Override public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                final Socket socket = serverSocket.accept();  // accept() blocks!
                final SocketClient socketClient = new SocketClient(socket, server);
                logger.info("Got client from {}", socket.getRemoteSocketAddress());
                final Future<?> clientFuture = executorService.submit(socketClient);
                socketClients.add(socketClient);
                submitted.add(clientFuture);
            }
        } catch (IOException e) {
            logger.error("Error in accpting clients to server socket", e);
        }
    }

    @Override public void close() throws IOException {
        for (SocketClient socketClient : socketClients) {
            socketClient.close();
        }
        for (Future<?> f : submitted) {
            f.cancel(true);
        }
        executorService.shutdownNow();
    }
}