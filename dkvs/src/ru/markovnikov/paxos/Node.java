package ru.markovnikov.paxos;

import ru.markovnikov.messages.*;
import ru.markovnikov.utils.PropertiesParser;
import ru.markovnikov.utils.Container;
import ru.markovnikov.utils.MyLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

/**
 * Created by nikita on 17.05.16.
 */
public class Node implements Runnable, AutoCloseable {
    public static PropertiesParser propertiesParser = null;
    private int id;
    private ServerSocket inSocket = null;
    private volatile boolean started = false;
    private volatile boolean stopped = false;
    private LinkedBlockingDeque<Message> messages = new LinkedBlockingDeque<>();
    private HashMap<Integer, Entry> nodes;
    private SortedMap<Integer, Entry> clients = new TreeMap<>();
    private Replica replica;
    private Acceptor acceptor;
    private Leader leader;
    public Container container;
    public MyLogger logger;
    private Timer timer;


    public static void setPropertiesParser(PropertiesParser propertiesParser) {
        Node.propertiesParser = propertiesParser;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ServerSocket getInSocket() {
        return inSocket;
    }

    public void setInSocket(ServerSocket inSocket) {
        this.inSocket = inSocket;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public LinkedBlockingDeque<Message> getMessages() {
        return messages;
    }

    public void setMessages(LinkedBlockingDeque<Message> messages) {
        this.messages = messages;
    }

    public HashMap<Integer, Entry> getNodes() {
        return nodes;
    }

    public void setNodes(HashMap<Integer, Entry> nodes) {
        this.nodes = nodes;
    }

    public SortedMap<Integer, Entry> getClients() {
        return clients;
    }

    public void setClients(SortedMap<Integer, Entry> clients) {
        this.clients = clients;
    }

    public Replica getReplica() {
        return replica;
    }

    public void setReplica(Replica replica) {
        this.replica = replica;
    }

    public Acceptor getAcceptor() {
        return acceptor;
    }

    public void setAcceptor(Acceptor acceptor) {
        this.acceptor = acceptor;
    }

    public Leader getLeader() {
        return leader;
    }

    public void setLeader(Leader leader) {
        this.leader = leader;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public void setLogger(MyLogger logger) {
        this.logger = logger;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    private class Entry {
        public Socket in = null;
        public Socket out = null;
        public LinkedBlockingDeque<Message> messages = new LinkedBlockingDeque<>();
        public volatile boolean ready = false;
        public volatile boolean inputAlive = false;
        public volatile boolean outputAlive = false;

        public void reset() {
            try {
                if (out != null)
                    out.close();
            } catch (IOException e) {

              //  e.printStackTrace();
            }
            out = new Socket();
            ready = false;
            messages.retainAll(messages.stream()
                    .filter(x -> !(x instanceof PingMessage))
                    .collect(Collectors.toList()));
        }




        public void setIn(Socket in) {
            this.in = in;
        }


        public void setOut(Socket out) {
            this.out = out;
        }


        public void setMessages(LinkedBlockingDeque<Message> messages) {
            this.messages = messages;
        }



        public void setReady(boolean ready) {
            this.ready = ready;
        }
    }

    public void sendToClient(int to, Message message) {
        while (!stopped) {
            try {
                clients.get(to).messages.putLast(message);
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendToNode(int to, Message message) {
        while (!stopped) {
            try {
                if (to == id)
                    messages.put(message);
                else
                    nodes.get(to).messages.put(message);
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Node(int id) {
        this.id = id;
        this.container = new Container(id);
        try {
            this.logger = new MyLogger(id);
            if (propertiesParser == null) propertiesParser = PropertiesParser.readProperties();
            this.inSocket = new ServerSocket(propertiesParser.port(id));
            this.nodes = new HashMap<>(propertiesParser.nodesCount());
            this.replica = new Replica(id, this);
            this.leader = new Leader(id, this);
            this.acceptor = new Acceptor(id, this);

            for (int i = 0; i < propertiesParser.nodesCount(); i++) nodes.put(i, new Entry());

        } catch (IOException e) {
            logger.logError("Node()", e.getMessage());
        }
        timer = new Timer();
    }


    @Override
    public void run() {
        if (started) throw new IllegalStateException("Double starting is impossible");
        started = true;
        logger.logConnection("run()", "node is started");
        leader.start();
        for (int i = 0; i < propertiesParser.nodesCount(); i++) {
            if (i != id) {
                final int temp = i;
                new Thread(() -> speakToNode(temp)).start();
            }
        }
        new Thread(this::handleMessages).start();
        new Thread(() -> {
            while (!stopped) {
                try {
                    Socket client = inSocket.accept();
                    new Thread(() -> handleRequest(client)).start();
                } catch (IOException ignored) {

                }
            }
        }).start();
        TimerTask ping = new TimerTask() {
            @Override
            public void run() {
                nodes.entrySet().stream()
                        .filter(it -> (it.getKey() != id))
                        .filter(it -> it.getValue().ready)
                        .forEach(it -> {
                            if (!it.getValue().outputAlive)
                                sendToNode(it.getKey(), new PingMessage(id));
                            it.getValue().outputAlive = false;
                        });
            }
        };
        TimerTask faults = new TimerTask() {
            @Override
            public void run() {
                HashSet<Integer> faultyNodes = new HashSet<>();
                nodes.entrySet().stream()
                        .filter(it -> it.getKey() != id)
                        .forEach(it -> {
                            if (!it.getValue().inputAlive) {
                                if (it.getValue().in != null)
                                    try {
                                        it.getValue().in.close();
                                    } catch (IOException ignored) {
                                    }
                                faultyNodes.add(it.getKey());
                                logger.logConnection("monitorFaults()", "Node " + it.getKey() + " is faulty, closing its connection.");
                            }
                            it.getValue().inputAlive = false;
                        });

                if (faultyNodes.size() > 0)
                    leader.notifyFault(faultyNodes);
            }
        };
        timer.scheduleAtFixedRate(ping, propertiesParser.getTimeout(), propertiesParser.getTimeout());
        timer.scheduleAtFixedRate(faults, 4 * propertiesParser.getTimeout(), 4 * propertiesParser.getTimeout());
    }

    private void handleRequest(Socket client) {
        try {
            InputStreamReader reader = new InputStreamReader(client.getInputStream(), "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(reader);
            String msg = bufferedReader.readLine();
            String[] parts = msg.split(" ");

            logger.logMessageIn("handleRequest():", "GOT message [" + msg + "] with request");

            switch (parts[0]) {
                case "node":
                    int nodeId = Integer.parseInt(parts[1]);
                    try {
                        if (nodes.get(nodeId).in != null)
                            nodes.get(nodeId).in.close();
                    } catch (IOException ignored) {
                    }
                    nodes.get(nodeId).setIn(client);
                    logger.logConnection("handleRequest(node:" + nodeId + ")", String.format("#%d: Started listening to node.%d from %s", id, nodeId, client.getInetAddress()));
                    listenToNode(bufferedReader, nodeId);
                    break;
                case "get":
                case "set":
                case "delete":
                    final int newClientId;
                    if (clients.keySet().size() == 0) newClientId = 1;
                    else newClientId = (clients.keySet().stream().max(Comparator.<Integer>naturalOrder()).get()) + 1;

                    Entry entry = new Entry();
                    entry.setIn(client);
                    clients.put(newClientId, entry);
                    Message firstMessage = ClientRequest.parse(newClientId, parts);
                    sendToNode(id, firstMessage);
                    new Thread(() -> {
                        speakToClient(newClientId);
                    }).start();
                    logger.logConnection("handleRequest(client:" + newClientId + ")", String.format("Client %d connected to %d.", newClientId, id));
                    listenToClient(bufferedReader, newClientId);
                    break;
                default:
                    logger.logMessageIn("handleRequest( ... )", "something goes wrong: \"" + parts[0] + "\" received");
                    break;
            }

        } catch (IOException e) {
            logger.logError("handleRequest()", e.getMessage());
        }
    }

    private void handleMessages() {
        while (!stopped) {
            Message m = null;
            try {
                m = messages.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (m == null) continue;
            logger.logMessageIn("handleMessages()", String.format("Handling message: %s", m));
            if (m instanceof ReplicaMessage) {
                replica.receiveMessage((ReplicaMessage) m);
                continue;
            }
            if (m instanceof LeaderMessage) {
                leader.receiveMessage((LeaderMessage) m);
                continue;
            }
            if (m instanceof AcceptorMessage) {
                acceptor.receiveMessage((AcceptorMessage) m);
                continue;
            }
            logger.logMessageIn("handleMessages()", String.format("Unknown message: %s", m));
        }
    }

    @Override
    public void close() throws Exception {
        stopped = true;
        inSocket.close();
        for (Entry n : nodes.values()) {
            if (n.in != null) n.in.close();
            if (n.out != null) n.out.close();
        }

        for (Entry n : clients.values()) {
            if (n.in != null) n.in.close();
            if (n.out!= null) n.out.close();
        }
    }

    private void listenToNode(BufferedReader breader, int nodeId) {

        nodes.get(nodeId).inputAlive = true;
        while (!stopped) {
            try {
                String data = breader.readLine();
                nodes.get(nodeId).inputAlive = true;
                Message m = Message.parse(nodeId, data.split(" "));
                if (m instanceof PingMessage) {
                    sendToNode(m.getSource(), new PongMessage(id));
                    continue;
                }
                if (m instanceof PongMessage) continue;
                logger.logMessageIn("listenToNode(nodeId:" + nodeId + ")", "received message :" + m + " from " + nodeId);
                sendToNode(id, m);
            } catch (IOException e) {
                logger.logError("listenToNode(nodeId:" + nodeId + ")", nodeId + ": " + e.getMessage());
                break;
            }
        }
    }

    private void listenToClient(BufferedReader reader, Integer clientId) {
        logger.logConnection("listenToClient() in Node", String.format("#%d: Client %d connected", id, clientId));
        while (!stopped) {
            try {
                String fromClient = reader.readLine();
                if (fromClient == null)
                    throw new IOException("Client disconnected.");
                String[] parts = fromClient.split(" ");
                ClientRequest message = ClientRequest.parse(clientId, parts);
                if (message != null) {
                    logger.logMessageIn("listenToClient() in Node", String.format("received message %s from client %d", message, message.getSource()));
                    sendToNode(id, message);
                }
            } catch (IOException e) {
                logger.logError("listenToClient() in Node", String.format("Lost connection to Client %d: %s", clientId, e.getMessage()));
                break;
            } catch (IllegalArgumentException e) {
                sendToClient(clientId, new ClientResponse(id, e.getMessage()));
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("usage: Node i");
            System.exit(1);
        }
        int thisNodenumber = Integer.parseInt(args[0]);
        new Node(thisNodenumber).run();
    }

    private void speakToNode(int nodeId) {
        String address = propertiesParser.address(nodeId);
        int port = propertiesParser.port(nodeId);
        if (address == null) {
            logger.logError("speakToNode(nodeId" + nodeId + ") in Node", String.format("#%d: Couldn't get address for %d, closing.", id, nodeId));
            return;
        }

        while (!stopped) {
            try {
                nodes.get(nodeId).reset();
                Socket clientSocket = nodes.get(nodeId).out;

                clientSocket.connect(new InetSocketAddress(address, port));
                logger.logConnection("speakToNode(nodeId: " + nodeId + ")", String.format("#%d: CONNECTED to node.%d", id, nodeId));
                sendToNodeAtFirst(nodeId, new NodeMessage(id));
                logger.logMessageOut("speakToNode(nodeId: " + nodeId + ")", String.format("adding node %d to queue for %d", id, nodeId));
                OutputStreamWriter writer = new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8");
                nodes.get(nodeId).setReady(true);
                while (!stopped) {
                    nodes.get(nodeId).outputAlive = true;
                    Message m = null;
                    try {
                        m = nodes.get(nodeId).messages.takeFirst();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (m == null) continue;
                    try {
                        writer.write(m + "\n");
                        writer.flush();
                        if (!(m instanceof PingMessage) && !(m instanceof PongMessage))
                            logger.logMessageOut("speakToNode(nodeId: " + nodeId + ")", String.format("SENT to %d: %s", nodeId, m));
                    } catch (IOException ioe) {
                        logger.logError("speakToNode(nodeId: " + nodeId + ")", String.format("Couldn't send a message from %d to %d. Retrying.", id, nodeId));
                        nodes.get(nodeId).messages.addFirst(m);
                        break;
                    }
                }
            } catch (SocketException e) {
                logger.logError("speakToNode(nodeId: " + nodeId + ")", String.format("DISCONNECTION: Connection from %d to node.%d lost: %s", id, nodeId, e.getMessage()));
                try {
                    Thread.sleep(propertiesParser.getTimeout());
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } catch (IOException e) {
                logger.logError("speakToNode(nodeId: " + nodeId + ")", String.format("Connection from %d to node.%d lost: %s", id, nodeId, e.getMessage()));
            }
        }
    }

    private void speakToClient(int clientId) {
        try {
            Entry entry = clients.get(clientId);
            BlockingDeque<Message> queue = entry.messages;
            OutputStreamWriter writer = new OutputStreamWriter(entry.in.getOutputStream(), "UTF-8");
            while (!stopped) {
                Message m = null;
                try {
                    m = queue.take();
                } catch (InterruptedException ignored) {
                }
                if (m == null) continue;
                try {
                    logger.logMessageOut("speakToClient(clientId: " + clientId + ")", String.format("#%d: Sending to client %d: %s", id, clientId, m));
                    writer.write(String.format("%s\n", m));
                    writer.flush();
                } catch (IOException ioe) {
                    logger.logMessageOut("speakToClient(clientId: " + clientId + ")", "Couldn't send a message. Retrying.");
                    clients.get(clientId).messages.addFirst(m);
                }
            }
        } catch (IOException ignored) {
        }
    }


    public void sendToNodeAtFirst(int to, Message message) {
        while (!stopped) try {
            if (to == id)
                messages.putFirst(message);
            else
                nodes.get(to).messages.putFirst(message);
            break;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
