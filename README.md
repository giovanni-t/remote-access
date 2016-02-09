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

In order to use the application you need to run the server. The server will provide you with an ip address, which you need to insert into the first page of the app and click *connect to server*. Once you are connected you can choose a name, if you choose a name already taken you will be asked to provide a different one. If you do not choose a name, the first thing you write in the application will be your name.

Now you can start sending requests, request for periodic messages or provide your own GPS position.

This is done by first selecting the name of the device you would like to communicate with, or choosing *all* in order to broadcast. After doing this you will be provided with the option of sending a request, recuesting for periodic messages or send your own GPS position. When you for example choose to send a request, options on what you can request will appear. When the only option that remains is *;*, your message is ready to be send and you click *SEND*, do not include *;* before *SEND*.

You can also add more actions to your message by adding *;* an action message. After choosing *;* you continue as explained over, starting by selecting a device which will recive the action.

When you click *SEND* your request will be sent through the server to the devise you choose and you will recive a response with the data you requested.


### Protocol syntax

The syntaxt of the messages beeing sent over the server is of the form

/recipient_user/command/action/params/moreparams

The recipient_user field is used for the username of the user the command is beeing sent to. recipient_user can also be set to *all* in case of broadcast.

The command field is used for commands, like requests (Req), responses or execute (Exec).

The action field is used for the specific action of the command. E.g. Gps, Photo, Live stream or Network. 

The params and moreparams field is used for any parameters that the action migth need. This field will be specific for every different action.


### Features

##### Already implemented




##### Ideas on features that can be added



## License

Apache License, Version 2.0, January 2004

http://www.apache.org/licenses/


## Support

If you are having issues, please let us know, by posting an issue on this github page.
