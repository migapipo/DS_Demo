# Distributed-System
This project builds a simple multi-server system for broadcasting activity objects between a number of clients. 

The multi-server system will:
- load balance client requests over the servers, using a redirection mechanism to ask clients to reconnect to another server.
- allow clients to register a username and secret, that can act as an authentication mechanism. Clients can login and logout as either anonymous or using a username/secret pair.
- allow clients to broadcast an activity object to all other clients connected at the time

The system was envisioned to allow clients to broadcast "activity stream objects" to each other. The activity stream format is defined at http://activitystrea.ms/


However, processing activity stream objects is beyond the scope of the project and only a simple process is required in this project (to authenticate the user who broadcast the object).

# Achitecture
- The multi-server system will form a tree: Skeleton code will be provided that will handle all of the socket connections to do this, as well as command line argument parsing and logging for diagnostic output.
- Each client will connect to a single server, and may be redirected to another server as part of the
process.
# Interaction
- All communication will be via TCP sockets.
- The architecture allows a simple broadcast communication between servers, so that a message from a
server can be received by all other servers without duplication. This is a network overlay.
- Each client communicates with a selected server.
- All messages will be sent as JSON objects with a newline to delimit them, using Java UTF8-modified,
which is the default for Java.
- A mixture of request and request/reply protocols are used.
- Activity stream objects are also JSON objects and are the "payload" of some messages in the
mult-server system.
