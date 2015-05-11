package net.gcdc.ittgt.client.gpsd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GpsdServerTest {

    @Test(timeout = 2000)
    public void test() throws UnknownHostException,
            IOException,
            InterruptedException,
            ExecutionException {
        final int port = 4003;
        final GpsdServer gpsdServer = GpsdServer.start(port);

        final Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream()));
        final Gson gson = new GsonBuilder().create();
        final double lat = 57;
        final double lon = 13;
        gpsdServer.setLastSeen(GpsData.builder().lat(lat).lon(lon).create());

        // Test with Plugtest server: "nc 212.234.160.4 1941"
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final Collection<Throwable> exceptions = new ArrayList<>();

//        System.err.println("Send");
        writer.write("?WATCH={\"enable\":true,\"json\":true};\n");  // Send us updates automatically.
        writer.flush();
//        System.err.println("Setting last seen");
        gpsdServer.setLastSeen(GpsData.builder().lat(lat).lon(lon).create());

//        System.err.println("Starting");
        Future<?> runner = executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean seenTpv = false;
//                    System.err.println("loop");
                    while (true) {
//                        System.err.println("readline");
//                        System.err.println("Avail: " + socket.getInputStream().available());
//                        Thread.sleep(20);
//                        System.err.println("Avail: " + socket.getInputStream().available());
//                        Thread.sleep(20);
//                        System.err.println("Avail: " + socket.getInputStream().available());
//                        Thread.sleep(20);
//                        System.err.println("Avail: " + socket.getInputStream().available());
                        String line = reader.readLine();
//                        System.err.println("line: " + line);
                        if (line == null) {
                            fail("failed to read line");
                            break;
                        }
                        if (line.startsWith("{\"class\":\"TPV\"")) {
                            TPV msg = gson.fromJson(line, TPV.class);
                            assertEquals(msg.lat(), lat, 0.1);
                            assertEquals(msg.lon(), lon, 0.1);
                            seenTpv = true;
                            break;
                        } else {
                            // Ignore all other non-TPV messages.
                            //System.err.println(line);
                        }
                    }
                    if (!seenTpv) {
                        fail("never seen tpv!");
                    }
                } catch (Throwable e) {
                    exceptions.add(e);
                }
            }
        });
        Thread.sleep(30);
        gpsdServer.setLastSeen(GpsData.builder().lat(lat).lon(lon).create());
        runner.get();
        assertTrue("failed with exception(s)" + exceptions, exceptions.isEmpty());
        reader.close();
        writer.close();
        socket.close();
        executor.shutdownNow();
    }

}
