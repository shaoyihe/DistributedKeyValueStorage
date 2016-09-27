package ru.markovnikov.paxos;

import ru.markovnikov.messages.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by nikita on 28.05.16.
 */
public class Leader {
    public int id;
    public Node machine;
    public List<Integer> replicaIds;
    public List<Integer> acceptorIds;
    public volatile boolean active;
    public volatile Ballot currentBallot;
    private HashMap<Integer, Command> proposals;
    private HashMap<ProposalValue, Commander> commanders;
    private HashMap<Ballot, Scout> scouts;
    private int awaitingToFault = -1;

    public Leader(int id, Node machine) {
        this.id = id;
        this.machine = machine;
        this.acceptorIds = Node.propertiesParser.ids();
        this.replicaIds = Node.propertiesParser.ids();
        proposals = new HashMap<>();
        currentBallot = new Ballot(machine.container.lastBallotNum, id);
        active = id == 0;
        commanders = new HashMap<>();
        scouts = new HashMap<>();
    }

    public void start() {
        startScouting(currentBallot);
    }

    public void receiveMessage(LeaderMessage message) {
        machine.logger.logPaxos("ReceiveMessage() in Leader", "received message:" + message + "");

        if (message instanceof ProposeMessage) {
            if (!proposals.containsKey(((ProposeMessage) message).getSlot())) {
                proposals.put(((ProposeMessage) message).slot, ((ProposeMessage) message).getRequest());
                if (active) {
                    command(new ProposalValue(currentBallot, ((ProposeMessage) message).slot,
                            ((ProposeMessage) message).getRequest()));
                } else {
                    machine.logger.logPaxos("ReceiveMessage() in Leader", "Leader " + id + " is not active.");
                }
            } else {
                machine.logger.logError("ReceiveMessage() in Leader", "slot " + ((ProposeMessage) message).slot + " is already used.");
            }
        }
        if (message instanceof PhaseOneResponse) {
            Ballot ballot = ((PhaseOneResponse) message).getOriginalBallot();
            Scout scout = scouts.get(ballot);
            scout.receiveResponse((PhaseOneResponse) message);
        }

        if (message instanceof PhaseTwoResponse) {
            ProposalValue proposal = ((PhaseTwoResponse) message).getProposalValue();
            Commander commander = commanders.get(proposal);
            commander.receiveResponse((PhaseTwoResponse) message);
        }

    }

    private void preempted(Ballot b) {
        machine.logger.logPaxos(String.format("PREEMPTED: there's ballot %s", b));
        if (b.compareTo(currentBallot) > 0) {
            active = false;
            machine.logger.logPaxos(String.format("LEADER %d is not active!", id));
            machine.logger.logPaxos(String.format("WAITING for %d to fail", b.leaderId));

            awaitingToFault = b.leaderId;

            currentBallot = new Ballot(machine.container.nextBallotNum(), id);
            machine.container.saveToDisk("ballot " + currentBallot);
        }
    }

    private void adopted(Ballot ballot, Map<Integer, ProposalValue> proposalValues) {
        machine.logger.logPaxos(String.format("ADOPTED with ballot %s", ballot));

        for (Map.Entry<Integer, ProposalValue> entry : proposalValues.entrySet()) {
            Integer key = entry.getKey();
            ProposalValue value = entry.getValue();
            proposals.put(key, value.command);
        }
        active = true;

        for (Map.Entry<Integer, Command> entry : proposals.entrySet()) {
            Integer key = entry.getKey();
            Command value = entry.getValue();
            command(new ProposalValue(ballot, key, value));
        }
    }


    public void setId(int id) {
        this.id = id;
    }


    public void setMachine(Node machine) {
        this.machine = machine;
    }


    public void setReplicaIds(List<Integer> replicaIds) {
        this.replicaIds = replicaIds;
    }


    public void setAcceptorIds(List<Integer> acceptorIds) {
        this.acceptorIds = acceptorIds;
    }


    public void setActive(boolean active) {
        this.active = active;
    }


    public void setCurrentBallot(Ballot currentBallot) {
        this.currentBallot = currentBallot;
    }

    public HashMap<Integer, Command> getProposals() {
        return proposals;
    }

    public void setProposals(HashMap<Integer, Command> proposals) {
        this.proposals = proposals;
    }

    public HashMap<ProposalValue, Commander> getCommanders() {
        return commanders;
    }

    public void setCommanders(HashMap<ProposalValue, Commander> commanders) {
        this.commanders = commanders;
    }

    public HashMap<Ballot, Scout> getScouts() {
        return scouts;
    }

    public void setScouts(HashMap<Ballot, Scout> scouts) {
        this.scouts = scouts;
    }

    public int getAwaitingToFault() {
        return awaitingToFault;
    }

    public void setAwaitingToFault(int awaitingToFault) {
        this.awaitingToFault = awaitingToFault;
    }

    private class Scout {
        HashSet<Integer> waitFor;
        HashMap<Integer, ProposalValue> proposals;
        Ballot b;

        public Scout(Ballot b) {
            this.b = b;
            waitFor = new HashSet<>(acceptorIds);
            proposals = new HashMap<>();
        }

        public void receiveResponse(PhaseOneResponse response) {
            if (response.getBallotNum().equals(b)) {
                response.getProposalValues().forEach(r ->
                        {
                            if ((!proposals.containsKey(r.slot)) || proposals.get(r.slot).ballotNum.less(r.ballotNum))
                                proposals.put(r.slot, r);
                        }
                );
                waitFor.remove(response.getSource());
                if (waitFor.size() < (acceptorIds.size() + 1) / 2) adopted(b, proposals);
            } else preempted(response.getBallotNum());
        }
    }

    private void startScouting(Ballot ballot) {
        scouts.put(ballot, new Scout(currentBallot));
        acceptorIds.forEach(a -> machine.sendToNode(a, new PhaseOneRequest(id, ballot)));
    }

    private class Commander {
        ProposalValue proposal;
        HashSet<Integer> waitFor;

        public Commander(ProposalValue proposal) {
            this.proposal = proposal;
            this.waitFor = new HashSet<>(acceptorIds);
        }

        public void receiveResponse(PhaseTwoResponse response) {
            if (response.getBallot().equals(currentBallot)) {
                waitFor.remove(response.getSource());
                if (waitFor.size() < (acceptorIds.size() + 1) / 2) {
                    replicaIds.forEach(r ->
                            machine.sendToNode(r, new DecisionMessage(response.getProposalValue().slot,
                                    response.getProposalValue().command))
                    );
                }
            } else {
                preempted(response.getBallot());
            }
        }
    }


    private void command(ProposalValue proposal) {
        machine.logger.logPaxos(String.format("Command() in leader started for %s", proposal));
        commanders.put(proposal, new Commander(proposal));
        acceptorIds.forEach(a -> machine.sendToNode(a, new PhaseTwoRequest(id, proposal)));
    }


    public void notifyFault(HashSet<Integer> faults) {
        if (!active && faults.contains(awaitingToFault)) {
            startScouting(currentBallot);
        }
    }
}
