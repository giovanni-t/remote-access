from twisted.internet.protocol import Factory, Protocol
from twisted.internet import reactor
from twisted.protocols.basic import LineReceiver
import socket,os,pprint

class Chat(LineReceiver):
    def __init__(self, clients, liveIps):
        self.clients = clients
        self.liveIps = liveIps
        self.name = None
        self.state = "GETNAME"

    def connectionMade(self):
        print "New client connecting..."
        self.sendLine("<server> //req/Hello/whatsyourname")

    def connectionLost(self, reason):
        if self.clients.has_key(self.name):
            left_name = self.name
            del self.clients[self.name]
            if self.liveIps.has_key(self.name):
                del self.liveIps[self.name]
            print left_name + ' has left'
            #self.broadcast( left_name + ' has left')
            self.send_clients_lists()
            self.send_liveIPs_lists()

    def handle_getname(self, name):
        if self.clients.has_key(name) or name == "server":
            self.sendLine("<server> //resp/nametaken")
            return
        self.name = name
        self.clients[name] = self
        self.state = "GETCOMMAND"
        send_msg = "<server> /%s/resp/Welcome!" % (name, )
        self.sendLine(send_msg )
        self.send_clients_lists()
        self.send_liveIPs_lists()

    def dataReceived(self, data):
        if self.state == "READATA":
            print data
            self.read_data(data)
            if data[-6:].rstrip(os.linesep) == "_end_":
                print "change_state"
                self.state = "GETCOMMAND"
        else:
            #strip the endings if they are '\n' and/or '\r'
            data = data.rstrip(os.linesep)
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

    def read_data(self,data):
        #print data
        for name, protocol in self.clients.iteritems():
            if protocol == self.clients[self.dest] :
                protocol.sendLine(data)

    def send_clients_lists(self):
        names = ''
        count = 0
        for name, protocol in self.clients.iteritems():
            names += '/' + protocol.name
            count +=1
        # names are already in the form "/name1/name2/...."
        message = "<server> /broadcast/resp/clientlist/%s%s" %(str(count), names)
        for name, protocol in self.clients.iteritems():
            protocol.sendLine(message)

    def send_liveIPs_lists(self):
        names = ''
        count = 0
        for name, ip in self.liveIps.iteritems():
            names += '/' + ip
            count +=1
        # names are already in the form "/name1/name2/...."
        message = "<server> /broadcast/resp/liveIps/%s%s" %(str(count), names)
        for name, protocol in self.clients.iteritems():
            protocol.sendLine(message)

    def handle_Command(self, msg_array, raw_msg):
        print "clients are %s " %self.clients
        if len(msg_array)>=3 and (msg_array[2] == 'req'or msg_array[2] == 'resp'or msg_array[2] == 'exec' or msg_array[2] == 'OK'):
            if self.clients.has_key(msg_array[1]):
                print "fowarding msg to %s " % self.clients[msg_array[1]].name
                if msg_array[2] == 'resp' and msg_array[3] == 'photo':
                    print "state Changed to READATA"
                    self.state = "READATA"
                    self.dest = msg_array[1]
                    self.message(msg_array[1], raw_msg)
                elif msg_array[2] == 'resp' and msg_array[3] == 'live':
                    self.liveIps[self.name] = msg_array[4]
                    #print "live IPs are %s" %self.liveIps
                    self.send_liveIPs_lists()
                else:
                    self.message(msg_array[1], raw_msg)
            else :
                print "client %s not found" % msg_array[1]
            	self.sendLine("<server> client %s not found" % msg_array[1])
        else :
            #self.broadcast(raw_msg)
            self.sendLine("<server> wrong command")


class ChatFactory(Factory):
    def __init__(self):
        self.clients = {} # maps user names to Chat instances
        self.liveIps = {}
    def buildProtocol(self, addr):
        return Chat(self.clients, self.liveIps)


ifaceListeningPort = reactor.listenTCP(45678, ChatFactory())
#3/12 by Alessio
#print "IP:", socket.gethostbyname(socket.gethostname())
#7/12 by Giovanni: found this huge oneliner to get the correct ip address instead of localhost/loopback address
ip = [l for l in ([ip for ip in socket.gethostbyname_ex(socket.gethostname())[2] if not ip.startswith("127.")][:1], [[(s.connect(('8.8.8.8', 80)), s.getsockname()[0], s.close()) for s in [socket.socket(socket.AF_INET, socket.SOCK_DGRAM)]][0][1]]) if l]
print "Chat server started on", socket.gethostname()+"/"
pprint.pprint(ip)

reactor.run()
