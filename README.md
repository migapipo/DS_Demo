# Distributed-System
This project builds a simple multi-server system for broadcasting activity objects between a number of clients. 

The multi-server system will:
♦ load balance client requests over the servers, using a redirection mechanism to ask clients to reconnect to another server.
♦ allow clients to register a username and secret, that can act as an authentication mechanism. Clients can login and logout as either anonymous or using a username/secret pair.
♦ allow clients to broadcast an activity object to all other clients connected at the time

The system was envisioned to allow clients to broadcast "activity stream objects" to each other. The activity stream format is defined at http://activitystrea.ms/


However, processing activity stream objects is beyond the scope of the project and only a simple process is required in this project (to authenticate the user who broadcast the object).
