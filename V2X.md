# A note on V2X part of Interactive Test Tool

Vehicle-to-Vehicle and Vehicle-to-Infrastructure (V2X) part of the Interactive Test Tool (ITT) is a simple Virtual Private Network (VPN) based on OpenVPN.

Just set up an OpenVPN server somewhere, and equip each ITT client with OpenVPN client. OpenVPN setup should use Layer 2 "TAP" connection (and not Layer 3 "TUN" connection) to be able to forward non-IP traffic (GeoNetworking is non-IP traffic). Then just send all GeoNetworking data to `tap0` instead of `wlan0`.

Setting up OpenVPN is documented in the OpenVPN [manuals page](https://openvpn.net/index.php/open-source/documentation/manuals/). OpenVPN requires [PKI](http://en.wikipedia.org/wiki/Public_key_infrastructure) (Public Key Infrastructure, see also [intro to PKI at easy-rsa](https://github.com/OpenVPN/easy-rsa/blob/master/doc/Intro-To-PKI.md)), you can set up one with [easy-rsa](https://github.com/OpenVPN/easy-rsa), see easy-rsa [quick-start](https://github.com/OpenVPN/easy-rsa/blob/master/README.quickstart.md) and [manual](https://github.com/OpenVPN/easy-rsa/blob/master/doc/EasyRSA-Readme.md).
