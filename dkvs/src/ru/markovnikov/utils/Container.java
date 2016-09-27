package ru.markovnikov.utils;

import ru.markovnikov.paxos.Ballot;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Created by nikita on 19.05.16.
 */
public class Container {
    public int nodeId;
    public String fileName;
    private BufferedWriter bufferedWriter = null;
    public volatile int lastBallotNum = 0;
    public volatile HashMap<String, String> keyValueStorage;
    public volatile int lastSlotOut = -1;

    public Container(int nodeId) {
        this.nodeId = nodeId;
        fileName = String.format("dkvs_%d.log", nodeId);
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(fileName, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        keyValueStorage = new HashMap<>();
        BufferedReader reader = null;
        try {
            File file = new File(fileName);
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("no log file");
            System.exit(1);
        }
        ArrayList<String> lines = reader.lines().collect(Collectors.toList()).stream().collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(lines);
        HashMap<String, String> temp = new HashMap<>();
        HashSet<String> removedKeys = new HashSet<>();
        cont:
        for (String l : lines) {
            String[] parts = l.split(" ");
            switch (parts[0]) {
                case "ballot":
                    lastBallotNum = Math.max(lastBallotNum, Ballot.parse(parts[1]).ballotNumber);
                    break;
                case "slot":
                    String key = (parts.length >= 5) ? parts[5] : null;
                    lastSlotOut = Math.max(lastSlotOut, Integer.parseInt(parts[1]));
                    if (temp.containsKey(key) || removedKeys.contains(key))
                        continue cont;
                    switch (parts[3]) {
                        case "set":
                            temp.put(key, parts[6]);
                            break;
                        case "delete":
                            removedKeys.add(key);
                            break;
                    }
                    break;
            }
        }

        keyValueStorage = temp;

        for (String l : lines) {
            String[] parts = l.split(" ");
            if (parts[0].equals("ballot")) {
                lastBallotNum = Ballot.parse(parts[1]).ballotNumber;
                break;
            }
        }
    }

    public void saveToDisk(String s) {
        try {
            bufferedWriter.write(s);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to write to file");
            System.exit(1);
        }
    }

    public int nextBallotNum() {
        return ++lastBallotNum;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public BufferedWriter getBufferedWriter() {
        return bufferedWriter;
    }

    public void setBufferedWriter(BufferedWriter bufferedWriter) {
        this.bufferedWriter = bufferedWriter;
    }

    public int getLastBallotNum() {
        return lastBallotNum;
    }

    public void setLastBallotNum(int lastBallotNum) {
        this.lastBallotNum = lastBallotNum;
    }

    public HashMap<String, String> getKeyValueStorage() {
        return keyValueStorage;
    }

    public void setKeyValueStorage(HashMap<String, String> keyValueStorage) {
        this.keyValueStorage = keyValueStorage;
    }

    public int getLastSlotOut() {
        return lastSlotOut;
    }

    public void setLastSlotOut(int lastSlotOut) {
        this.lastSlotOut = lastSlotOut;
    }
}
