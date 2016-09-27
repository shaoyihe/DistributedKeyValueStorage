package ru.markovnikov;

import ru.markovnikov.paxos.Node;

/**
 * Created by nikita on 29.05.16.
 */
public class Main {
    public static void main(String[] args) {
        for (int i = 0; i < 3; ++i) {
            new Thread(new Node(i)).start();
        }
    }
}
