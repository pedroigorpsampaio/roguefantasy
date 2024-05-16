package com.mygdx.server.ui;

import com.esotericsoftware.minlog.Log;
import com.mygdx.server.network.LoginServer;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Class that parses and processes console commands
 */
public class CommandDispatcher {
    private CmdReceiver consoleUI;
    private CmdReceiver loginServer;
    HashMap<CmdType, Integer> cmdArgsSize; // size of args for each command

    // constructor set references to the command receivers
    public CommandDispatcher(CmdReceiver consoleUI, CmdReceiver loginServer) {
        this.consoleUI = consoleUI;
        this.loginServer = loginServer;
        // sets arg size for each cmd
        cmdArgsSize = new HashMap<>();
        cmdArgsSize.put(CmdType.CLEAR_LOG, 1);
        cmdArgsSize.put(CmdType.BROADCAST, 1);
        cmdArgsSize.put(CmdType.RESTART, 1);
        cmdArgsSize.put(CmdType.START, 1);
        cmdArgsSize.put(CmdType.STOP, 1);
    }

    // parses the command string
    private Command parseCommand(String cmdStr) {
        String trimmedStr = cmdStr.trim();
        String[] splitData = trimmedStr.split("\\s+");
        Log.debug("cmd", Arrays.toString(splitData));
        Command cmd = new Command();
        switch (splitData[0]) {
            case "clear":
                cmd.type = CmdType.CLEAR_LOG;
                break;
            default:
                Log.debug("cmd", "Unknown command: "+splitData[0]);
                cmd.type = CmdType.UNKNOWN;
                break;
        }
        if(splitData.length > 1) // has args
            cmd.args = Arrays.copyOfRange(splitData, 1, splitData.length); // stores args

        return cmd;
    }

    /**
     * Process commands received via console
     * dispatching it to the correct receiver
     * @param cmdString the command string input of the console
     */
    public void processCommand(String cmdString) {
        Command cmd = parseCommand(cmdString); // parses the string into a command with arguments
        int caSize = cmd.args == null ? 0 : cmd.args.length;

        switch(cmd.type) {
            case CLEAR_LOG:
                // only process if args size is correct
                if(caSize == cmdArgsSize.get(CmdType.CLEAR_LOG))
                    consoleUI.process(cmd);
                else {
                    if(caSize > 0)
                        Log.info("cmd", "Wrong parameters: " + Arrays.toString(cmd.args));
                    else
                        Log.info("cmd", "Clear parameter not provided (all, global, login, chat, game)");
                }
                break;
            default:
                break;
        }
    }

    public interface CmdReceiver {
        void process(Command cmd);
    }

    public class Command {
        CmdType type;
        String[] args;
        public Command(){}
        public Command(CmdType type) {this.type = type; args = null;}
        public Command(CmdType type, String[] args) {this.type = type; this.args = args;}
        public CmdType getType() {return type;}
        public void setType(CmdType type) {this.type = type;}
        public String[] getArgs() {return args;}
        public void setArgs(String[] args) {this.args = args;}
    }

    enum CmdType {
        CLEAR_LOG,
        BROADCAST,
        START,
        STOP,
        RESTART,
        UNKNOWN
    }
}
