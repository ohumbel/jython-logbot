package org.jibble.logbot;

import java.io.*;
import java.util.*;

public class LogBotMain {

    public static void main(String[] args) throws Exception {
        String rootDir;
        if (args != null && args.length == 1) {
            rootDir = args[0];
        } else {
            rootDir = ".";
        }
        Properties p = new Properties();
        p.load(new FileInputStream(new File(rootDir.concat("/config.ini"))));
        String server = p.getProperty("Server", "localhost");
        String channel = p.getProperty("Channel", "#test");
        String nick = p.getProperty("Nick", "LogBot");
        String pass = p.getProperty("Pass", "thePassword");
        String joinMessage = p.getProperty("JoinMessage", "This channel is logged.");
        File outDir = new File(p.getProperty("OutputDir", rootDir.concat("/output/")));
        outDir.mkdirs();
        if (!outDir.isDirectory()) {
            System.out.println("Cannot make output directory (" + outDir + ")");
            System.exit(1);
        }
        LogBot.copy(new File(rootDir.concat("/html/header.inc.php")), new File(outDir, "header.inc.php"));
        LogBot.copy(new File(rootDir.concat("/html/footer.inc.php")), new File(outDir, "footer.inc.php"));
        LogBot.copy(new File(rootDir.concat("/html/index.php")), new File(outDir, "index.php"));
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outDir, "config.inc.php")));
        writer.write("<?php");
        writer.newLine();
        writer.write("    $server = \"" + server + "\";");
        writer.newLine();
        writer.write("    $channel = \"" + channel + "\";");
        writer.newLine();
        writer.write("    $nick = \"" + nick + "\";");
        writer.newLine();
        writer.write("?>");
        writer.flush();
        writer.close();
        LogBot bot = new LogBot(nick, outDir, joinMessage);
        bot.connect(server, pass);
        bot.joinChannel(channel);
    }
}