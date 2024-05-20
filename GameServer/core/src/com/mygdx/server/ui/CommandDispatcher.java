package com.mygdx.server.ui;

import com.esotericsoftware.minlog.Log;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Class that parses and processes console commands
 */
public class CommandDispatcher {
    private CmdReceiver consoleUI;

    private CmdReceiver loginServer;
    public CmdReceiver getLoginServer() {return loginServer;}
    public void setLoginServer(CmdReceiver loginServer) {this.loginServer = loginServer;}
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
        cmdArgsSize.put(CmdType.PLAY, 1);
        cmdArgsSize.put(CmdType.PAUSE, 0);
    }

    // parses the command string
    // returning a command object
    private Command parseCommand(String cmdStr) {
        String trimmedStr = cmdStr.trim();
        String[] splitData = trimmedStr.split("\\s+");

        Command cmd = new Command();
        switch (splitData[0]) {
            case "clear":
                cmd.type = CmdType.CLEAR_LOG;
                break;
            case "stop":
                cmd.type = CmdType.STOP;
                break;
            case "start":
                cmd.type = CmdType.START;
                break;
            case "restart":
                cmd.type = CmdType.RESTART;
                break;
            case "play":
                cmd.type = CmdType.PLAY;
                break;
            case "pause":
                cmd.type = CmdType.PAUSE;
                break;
            default:
                Log.info("cmd", "Unknown command: "+splitData[0]);
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

        // returns if command is unknown
        if(cmd.type == CmdType.UNKNOWN) return;

        // only process if args size is correct
        if (caSize != cmdArgsSize.get(cmd.type)) {
            if (caSize > 0)
                Log.info("cmd", "Wrong parameters: " + Arrays.toString(cmd.args));
            else
                Log.info("cmd", cmd.type.text + " valid parameter not provided "+cmd.type.getPossibleArgs());
            return;
        }

        // distribute command to the interested command processors
        if(cmd.type == CmdType.CLEAR_LOG || cmd.type == CmdType.PLAY || cmd.type == CmdType.PAUSE) { // server console only commands
            consoleUI.process(cmd); // sends command to be processed by console ui
        } else if(cmd.type == CmdType.START || cmd.type == CmdType.STOP || cmd.type == CmdType.RESTART) { // server commands
            RogueFantasyServer.ServerChannel receiver = getReceiver(cmd);
            switch(receiver) { // sends command to the intended receiver
                case LOGIN:
                    loginServer.process(cmd);
                    consoleUI.process(cmd);
                    break;
                case CHAT: // TODO WHEN CHAT SERVER IS IMPLEMENTED
                    consoleUI.process(cmd);
                    break;
                case GAME: // TODO WHEN GAME SERVER IS IMPLEMENTED
                    consoleUI.process(cmd);
                    break;
                case ALL: // TODO WHEN ALL SERVERS ARE IMPLEMENTED
                    loginServer.process(cmd);
                    consoleUI.process(cmd);
                    break;
                default:
                    Log.info("cmd", "Invalid server: "+cmd.args[0]);
                    break;
            }
        }

    }

    public RogueFantasyServer.ServerChannel getReceiver(Command cmd) {
        RogueFantasyServer.ServerChannel receiver;
        try {
            receiver = RogueFantasyServer.ServerChannel.fromString(cmd.args[0]);
        } catch (Exception e) {
            receiver = RogueFantasyServer.ServerChannel.UNKNOWN;
        }
        return receiver;
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

    public enum CmdType {
        CLEAR_LOG("clear", "(all, global, login, chat, game)"),
        BROADCAST("broadcast", "(\"message\")"),
        START("start", "(all, login, chat, game)"),
        STOP("stop", "(all, login, chat, game)"),
        RESTART("restart", "(all, login, chat, game)"),
        PLAY("play", "(time_commando, corneria, demon_blue)"),
        PAUSE("pause", ""),
        UNKNOWN("unknown", "");
        private String text;
        private String args;

        CmdType(String text, String args) {
            this.text = text;
            this.args = args;
        }

        public String getText() {
            return this.text;
        }

        public static CmdType fromString(String text) throws Exception{
            for (CmdType t : CmdType.values()) {
                if (t.text.equalsIgnoreCase(text)) {
                    return t;
                }
            }
            throw new Exception("No enum constant with text " + text + " found");
        }

        public String getPossibleArgs() {
            return args;
        }
    }
}
