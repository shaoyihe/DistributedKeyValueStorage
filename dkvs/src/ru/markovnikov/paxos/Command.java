package ru.markovnikov.paxos;

import ru.markovnikov.messages.ClientRequest;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Created by nikita on 17.05.16.
 */
public class Command {
    public int operationId;
    private static volatile int nextId = 0;
    public ClientRequest request;

    public Command(int nodeId, ClientRequest request) {
        int current = get();
        this.operationId = current * Node.propertiesParser.nodesCount() + nodeId;
        this.request = request;
    }

    public Command(ClientRequest request, int operationId) {
        this.operationId = operationId;
        this.request = request;
    }

    public static synchronized int get() {
        return nextId++;
    }

    public static Command parse(String[] splitted) {
        String[] end = Arrays.copyOfRange(splitted, 3, splitted.length);
        String[] begin = new String[1];
        begin[0] = splitted[1];
        String[] both = Stream.concat(Arrays.stream(begin), Arrays.stream(end)).toArray(String[]::new);
        return new Command(ClientRequest.parse(Integer.parseInt(splitted[2]), both), Integer.parseInt(splitted[0].substring(1, splitted[0].length() - 1)));

    }

    @Override
    public String toString() {
        return "[" + operationId + "] " + request.toString();
    }

    @Override
    public boolean equals(Object o) {
        return this.toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public void setOperationId(int operationId) {
        this.operationId = operationId;
    }

    public static int getNextId() {
        return nextId;
    }

    public static void setNextId(int nextId) {
        Command.nextId = nextId;
    }

    public void setRequest(ClientRequest request) {
        this.request = request;
    }
}
