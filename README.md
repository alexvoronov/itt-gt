# Interactive Test Tool - Ground Truth part [![Build Status](https://travis-ci.org/alexvoronov/itt-gt.svg?branch=master)](https://travis-ci.org/alexvoronov/itt-gt) [![Coverage Status](https://coveralls.io/repos/alexvoronov/itt-gt/badge.svg)](https://coveralls.io/r/alexvoronov/itt-gt)

An implementation of Interactive Test Tool (ITT). ITT is outlined in the [ITT paper](https://raw.githubusercontent.com/alexvoronov/itt-gt/master/doc/Interactive.Test.Tool.preprint.pdf). The code here contains:

  - a Java model of the Ground Truth (GT) data to be exanged (we specified that GT is sent as JSON, here is a Java wrapper for that spec)
  - an ITT Ground Truth server
  - an example of a Ground Truth Client Adapter. 


Client itself (vehicle, implemented in Simulink) is not part of this code. V2X part of the ITT is not part of this code either, see [V2X](https://github.com/alexvoronov/itt-gt/blob/master/doc/V2X.md) for details about V2X part.


### Java model of GT

Java model is just a simple [Vehicle](https://github.com/alexvoronov/itt-gt/blob/master/src/main/java/net/gcdc/ittgt/model/Vehicle.java) class ([POJO](http://en.wikipedia.org/wiki/Plain_Old_Java_Object)) that is encoded to [JSON](http://en.wikipedia.org/wiki/JSON) by [GSON](https://github.com/google/gson). GT data of all clients constitutes a [WorldModel](https://github.com/alexvoronov/itt-gt/blob/master/src/main/java/net/gcdc/ittgt/model/WorldModel.java).

### ITT GT Server

Server listens on TCP for GT data from clients, then bundles it into WorldModel data, and sends it back to each client.

### ITT GT Client Adapter

Client adapter listens on a UDP port for data from Simulink. Adapter unpacks Simulink data from UDP, then repacks it into JSON and sends it over TCP to the ITT GT Server. 

Adapter receives WorldModel from ITT GT Server as JSON, unpacks JSON, then repacks the data as a UDP payload and sends it over UDP back to Simulink.


### Acknowledgements
This implementation was partly developed within [i-GAME](http://gcdc.net/i-game) project that has received funding from the European Union's Seventh Framework Programme for research, technological development and demonstration under grant agreement no [612035](http://cordis.europa.eu/project/rcn/110506_en.html).


### License

This code is released under the business-friendly Apache 2.0 license.
