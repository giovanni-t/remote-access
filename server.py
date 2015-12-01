from twisted.internet.protocol import Factory, Protocol
from twisted.internet import reactor
from twisted.protocols.basic import LineReceiver

class Chat(LineReceiver):
    def __init__(self, clients):
        self.clients = clients
        self.name = None
        self.state = "GETNAME"

    def connectionMade(self):
        print "clients make connection "
        self.sendLine("Hello, please choose a name")


    def connectionLost(self, reason):
        if self.clients.has_key(self.name):
            left_name = self.name
            del self.clients[self.name]
            self.broadcast( left_name + ' has left')


    def handle_getname(self, name):
        if self.clients.has_key(name):
            self.sendLine("name taken, please choose another.")
            return
        self.name = name
        self.clients[name] = self
        self.state = "GETCOMMAND"
        send_msg = "<%s> Welcome!" % (name, )
        self.sendLine(send_msg )
        self.send_clients_lists()


    def dataReceived(self, data):
        a = data.split('/')
        print a
        if len(a) >= 1:
            msg = ""
            if self.state == "GETNAME":
                self.handle_getname(a[0])

            elif self.state =="GETCOMMAND":
                self.handle_Command(a, data)


    #send to specific client
    def message(self, send_to, message):
        message = "<%s> %s" % (self.name, message)
        for name, protocol in self.clients.iteritems():
            if protocol == self.clients[send_to] :
                protocol.sendLine(message)

    def broadcast(self, message):
        #self.transport.write(message + '\n')
        message = "<%s> %s" % (self.name, message)
        for name, protocol in self.clients.iteritems():
            if protocol != self:
                protocol.sendLine(message)

    def send_clients_lists(self):
        names = ''
        count = 0
        for name, protocol in self.clients.iteritems():
            names = protocol.name + '/ '+ names
            count +=1
        message = "<server> there are %s clients. %s" %(str(count), names)
        for name, protocol in self.clients.iteritems():
            protocol.sendLine(message)

    def handle_Command(self, msg_array, raw_msg):
        print "clients are %s " %self.clients
        if len(msg_array)>=3 and (msg_array[2] == 'read'or msg_array[2] == 'write'or msg_array[2] == 'exec'):
            if self.clients.has_key(msg_array[1]):
                print "fowarding msg to %s " % self.clients[msg_array[1]].name
                self.message(msg_array[1], raw_msg)
            else :
                #print "client %s not found. Broadcasting the message" % msg_array[1]
                #self.broadcast(raw_msg)
                print "client %s not found" % msg_array[1]
            	self.message(self.name, "client %s not found" % msg_array[1])
        else :
            #self.broadcast(raw_msg)
            self.message(self.name, 'wrong command')
        

class ChatFactory(Factory):

    def __init__(self):
        self.clients = {} # maps user names to Chat instances

    def buildProtocol(self, addr):
        return Chat(self.clients)


reactor.listenTCP(45678, ChatFactory())
print "Chat server started"
reactor.run()
