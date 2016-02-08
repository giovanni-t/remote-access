# Drone Controller

University project for MobServ class at Eurecom.

The purpose of the project is to build a client-server application that allows to send remote commands between Android devices, having one (or more) of them onboard on drones.
The server is a simple chat server in Python/Twisted, and the client is an Android app that sends/receives messages and performs actions described there.

Supported features so far are requests of gps position, photo shooting and live streaming.

Potential new features include a remote pilot for the drones.


### How to install

In order to use the application, download the code and compile it using Android Studio.

For the server, python and the Twisted protocol are required. To launch the server use command
 
*$ python server.py*

When the server is up and running it will show the ip address to insert in the application.


### How to use

In order to use the application you need to run the server. The server will provide you with an ip address, which you need to insert into the first page of the app and click connect to the server.
Once you are connected you can choose a name, if you choose a name already taken you will be asked to provide a different one.

Now you can start sending requests, you do this by first choosing the name of the device you want to recive data from, or choosing to recive from all. Next choose what the request is, if it is a request for reciving the gps position, a picture or stream video. Then send the request by clicking send.

You can also send more requests at the same time, you do this by choosing ; after the choosen request. Then you continue by choosing the devise as before.

You can also send your own position by choosing send position after choosing a devise to send to.
