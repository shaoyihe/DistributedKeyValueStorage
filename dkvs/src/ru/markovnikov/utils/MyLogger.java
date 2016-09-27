package ru.markovnikov.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nikita on 07.05.16.
 */
public class MyLogger {
    private Logger logger;
    public MyLogger(int id) {
        this.logger = Logger.getLogger("node." + id);
    }

    public void logConnection(String src, String dst) {
        logger.info(src + ":\n-> " + dst);
    }
    public void logMessageOut(String src, String message) {
        logger.info(src + ":\n-> " + message);
    }

    public void logMessageIn(String src, String message) {
        logger.info(src + ":\n-> " + message);
    }

    public void logPaxos(String src, String s) {
        logger.info(src + ":\n-> " + s);
    }

    public void logPaxos(String message) {
        logger.info("paxos message" + ":\n-> " + message);
    }

    public void logError(String src, String message) {
        logger.log(Level.INFO, src + ":\n Error: " + message);
    }
}
