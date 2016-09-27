package ru.markovnikov.paxos;

import ru.markovnikov.messages.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by nikita on 19.05.16.
 */
public class Replica {
    public int id;
    public List<Integer> leaderIds;
    private Node machine;
    public volatile int slotIn = 0;
    public volatile int slotOut = 0;
    private HashMap<String, String> state;
    private HashSet<Command> requests = new HashSet<>();
    private HashMap<Integer, Command> proposals = new HashMap<>();
    private HashMap<Integer, Command> decisions = new HashMap<>();
    private HashMap<Command, Integer> awaitingClients = new HashMap<>();
    private HashSet<Command> performed = new HashSet<>();

    public Replica(int id, Node machine) {
        this.id = id;
        this.machine = machine;
        this.leaderIds = Node.propertiesParser.ids();
        state = machine.container.keyValueStorage;
        slotOut = machine.container.lastSlotOut + 1;
        slotIn = slotOut;
    }

    public void receiveMessage(ReplicaMessage message) {
        if (message instanceof GetRequest) {
            String key = ((GetRequest) message).getKey();
            String value = state.get(key);
            if (value == null)
                value = "NOT_FOUND";
            else
                value = "VALUE " + value;

            machine.sendToClient(message.getSource(), new ClientResponse(message.getSource(), value));
            return;
        } else if (message instanceof ClientRequest) {
            Command command = new Command(id, (ClientRequest) message);
            requests.add(command);
            awaitingClients.put(command, message.getSource());
        }

        if (message instanceof DecisionMessage) {
            Command actualRequestCommand = ((DecisionMessage) message).getRequest();
            int actualSlot = ((DecisionMessage) message).getSlot();

            machine.logger.logPaxos("ReceiveMessage(message) in Replica", String.format("decision %s", message));
            decisions.put(actualSlot, actualRequestCommand);

            while (decisions.containsKey(slotOut)) {
                Command command = decisions.get(slotOut);
                if (proposals.containsKey(slotOut)) {
                    Command proposalCommand = proposals.get(slotOut);
                    proposals.remove(slotOut);
                    if (!command.equals(proposalCommand)) {
                        requests.add(proposalCommand);
                    }
                }
                perform(command);
                ++slotOut;
            }
        }
        propose();
    }

    private void propose() {
        while (!requests.isEmpty()) {
            Command command = requests.iterator().next();
            machine.logger.logPaxos("propose() in Replica", String.format("propose %s to slot %d", command, slotIn));
            if (!decisions.containsKey(slotIn)) {
                requests.remove(command);
                proposals.put(slotIn, command);
                leaderIds.forEach(l -> machine.sendToNode(l, new ProposeMessage(id, slotIn, command)));
            }
            slotIn++;
        }
    }

    private void perform(Command command) {
        machine.logger.logPaxos("perform() in Replica", String.format("perform %s at %d", command, slotOut));
        if (performed.contains(command))
            return;

        if (command.request instanceof SetRequest) {
            state.put(((SetRequest) command.request).getKey(), ((SetRequest) command.request).getValue());
            Integer awaitingClient = awaitingClients.get(command);
            if (awaitingClient != null) {
                machine.sendToClient(awaitingClient, new ClientResponse(command.request.getSource(), "STORED"));
                awaitingClients.remove(awaitingClient);// !!!
            }
        }
        if (command.request instanceof DeleteRequest) {
            if (performed.contains(command))
                return;
            boolean result = state.containsKey(((DeleteRequest) command.request).getKey());
            state.remove(((DeleteRequest) command.request).getKey());
            ClientResponse resp = new ClientResponse(command.request.getSource(), (result) ? "DELETED" : "NOT_FOUND");
            Integer awaitingClient = awaitingClients.get(command);
            if (awaitingClient != null) {
                machine.sendToClient(awaitingClient, resp);
                awaitingClients.remove(awaitingClient);
            }
        }
        performed.add(command);
        if (!(command.request instanceof GetRequest))
            machine.container.saveToDisk(String.format("slot %d %s", slotOut, command));
    }


    public void setId(int id) {
        this.id = id;
    }


    public void setLeaderIds(List<Integer> leaderIds) {
        this.leaderIds = leaderIds;
    }

    public Node getMachine() {
        return machine;
    }

    public void setMachine(Node machine) {
        this.machine = machine;
    }

    public void setSlotIn(int slotIn) {
        this.slotIn = slotIn;
    }

    public void setSlotOut(int slotOut) {
        this.slotOut = slotOut;
    }

    public HashMap<String, String> getState() {
        return state;
    }

    public void setState(HashMap<String, String> state) {
        this.state = state;
    }

    public HashSet<Command> getRequests() {
        return requests;
    }

    public void setRequests(HashSet<Command> requests) {
        this.requests = requests;
    }

    public HashMap<Integer, Command> getProposals() {
        return proposals;
    }

    public void setProposals(HashMap<Integer, Command> proposals) {
        this.proposals = proposals;
    }

    public HashMap<Integer, Command> getDecisions() {
        return decisions;
    }

    public void setDecisions(HashMap<Integer, Command> decisions) {
        this.decisions = decisions;
    }

    public HashMap<Command, Integer> getAwaitingClients() {
        return awaitingClients;
    }

    public void setAwaitingClients(HashMap<Command, Integer> awaitingClients) {
        this.awaitingClients = awaitingClients;
    }

    public HashSet<Command> getPerformed() {
        return performed;
    }

    public void setPerformed(HashSet<Command> performed) {
        this.performed = performed;
    }
}
