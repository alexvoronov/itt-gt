# Interactive Test Tool - Ground Truth part [![Build Status](https://travis-ci.org/alexvoronov/itt-gt.svg?branch=master)](https://travis-ci.org/alexvoronov/itt-gt) [![Coverage Status](https://coveralls.io/repos/alexvoronov/itt-gt/badge.svg?branch=master)](https://coveralls.io/r/alexvoronov/itt-gt?branch=master)

An implementation of the Interactive Test Tool (ITT) for interoperability testing of Cooperative [Intelligent Transport Systems](http://en.wikipedia.org/wiki/Intelligent_transportation_system) (C-ITS). The ITT allows remote interoperability testing of e.g. [platooning](http://en.wikipedia.org/wiki/Platoon_%28automobile%29). Since the ITT considers each client as a black box, manufacturers can test together without revealing internal implementations to each other. ITT clients can be implemented as Model-in-the-Loop, Controller-in-the-Loop or even [Hardware-in-the-Loop](http://en.wikipedia.org/wiki/Hardware-in-the-loop_simulation), thus allowing to combine physical and virtual vehicles. For more details, see [ITT paper](https://github.com/alexvoronov/itt-gt/blob/master/doc/Interactive.Test.Tool.preprint.pdf).


## Status
The code here contains:

  - ITT Ground Truth server.
  - Example of an ITT Ground Truth Client Adapter. 


The code here does not contain the Client itself (vehicle, implemented in e.g. Simulink). 
The code here does not contain V2X part either, see [V2X notes](https://github.com/alexvoronov/itt-gt/blob/master/doc/V2X.md) for V2X part.



## Running

For Java sources, Maven is used as a build tool.

1. Start a server.

    Here's how to start an ITT Server on port 5001 with the initial WorldModel defined in `test1.json`:

    ```shell
    mvn exec:java -Dexec.mainClass="net.gcdc.ittgt.server.BasicGroTrServer" -Dexec.args="5001 src/test/resources/test1.json"
    ```

1. Start a client adapter.
 
   Here's how to start a client adapter connected to the ITT server on TCP 127.0.0.1:5001, listening for Simulink client on UDP 9080 and sending back to Simulink to UDP 127.0.0.1:9081:

    ```shell
    mvn exec:java -Dexec.mainClass="net.gcdc.ittgt.client.GroTrClient" -Dexec.args="--localPortForSimulink=9080 --remoteSimulinkAddress=127.0.0.1:9081 --grotrServerAddress=127.0.0.1:5001"
    ```

1. Start the client itself.

    If using Simulink, start a simulation using the GUI.



## Implementation

### ITT GT Server

Server listens on TCP for GT data from clients, then bundles it into WorldModel data, and sends it back to each client.

### ITT GT Client Adapter

Client adapter listens on a UDP port for data from Simulink. Adapter unpacks Simulink data from UDP, then repacks it into JSON and sends it over TCP to the ITT GT Server. 

Adapter receives WorldModel from ITT GT Server as JSON, unpacks JSON, then repacks the data as a UDP payload and sends it over UDP back to Simulink.

### Java model of GT

Java model is just a simple [Vehicle](https://github.com/alexvoronov/itt-gt/blob/master/src/main/java/net/gcdc/ittgt/model/Vehicle.java) class ([POJO](http://en.wikipedia.org/wiki/Plain_Old_Java_Object)) that is encoded to [JSON](http://en.wikipedia.org/wiki/JSON) by [GSON](https://github.com/google/gson). GT data of all clients constitutes a [WorldModel](https://github.com/alexvoronov/itt-gt/blob/master/src/main/java/net/gcdc/ittgt/model/WorldModel.java).


## Acknowledgements
This implementation was partly developed within [i-GAME](http://gcdc.net/i-game) project that has received funding from the European Union's Seventh Framework Programme for research, technological development and demonstration under grant agreement no [612035](http://cordis.europa.eu/project/rcn/110506_en.html).


## License

This code is released under the business-friendly Apache 2.0 license.
