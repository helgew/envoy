==================
Envoy Proxy Server
==================

This project provides an all-in-one solution to proxy and parse the data send from an Enphase Envoy appliance to the Enphase server. Most of the classes in the non-envoy packages can be used in similar projects.

Requirements
============

The project assumes deployment to a Unix platform but will work on any other platform that supports java with only minor modifications (i.e. path to the logging directory). 

All external API dependencies are managed via maven, which is needed for compilation. 

To re-direct communications from the Envoy appliance, iptables or similar is needed.

Out of the box, the project is configured to save data to a mysql database running on the same host as the proxy server.

Installation
=============

Compilation
-----------

* Change the user name and password as well as the database name in both `pom.xml` and `src/main/resources/torque.properties`.
* Generate the executable jar::

  `mvn package`
    
* Create the envoy database::

  `echo "create database envoy;" | mysql -u<user> -p<pass>`

* Generate the schema::

  `mvn sql:execute`

iptables
--------

You will need to set up an iptables `REDIRECT` rule in order to
intercept the outbound connections::

  `iptables -t nat -A PREROUTING -s <your_envoy_address> \
    -p tcp --dport 443 -j REDIRECT --to-ports 7777`

This will accept packets from your Envoy to port 443 and redirect them
to port 7777.

Configuration
=============

The main class in the one-jar jar generated in `target/EnvoyProxyServer.jar` accepts the following run-time arguments:

    Option                                  Description                            
    ------                                  -----------                            
    --help                                  display help text                      
    --load-files                            load files from optional directory or default directory if none given      
    --local-host                            local host address to bind to (default: localhost)                 
    --local-port <Integer>                  local port to listen to (default: 7777)
    --remote-url                            remote URL to proxy (default: https://reports.enphaseenergy.com)         

