package org.jibble.logbot;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.SimpleDateFormat;
import org.jibble.pircbot.*;

public class LogBot extends PircBot {

    private static final Pattern urlPattern = Pattern.compile("(?i:\\b((http|https|ftp|irc)://[^\\s]+))");

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("H:mm");

    public static final String GREEN = "irc-green";

    public static final String BLACK = "irc-black";

    public static final String BLACK_BOLD = "irc-black-bold";

    public static final String BROWN = "irc-brown";

    public static final String NAVY = "irc-navy";

    public static final String BRICK = "irc-brick";

    public static final String RED = "irc-red";

    private static final int DEFAULT_PORT = 6667; // see PircBot.connect(hostname)

    private File _outDir;

    private String _joinMessage;

    public LogBot(String name, File outDir, String joinMessage) {
	setName(name);
	setVerbose(true);
	_outDir = outDir;
	_joinMessage = joinMessage;
    }

    private void append(String color, String line) {
	Date now = new Date();
	try {
	    append(color, line, getFileWriter(now), now);
	} catch (IOException e) {
	    System.out.println("Could not write to log: " + e);
	}
    }

    public void append(String color, String line, Writer writer, Date now) throws IOException {
	line = Colors.removeFormattingAndColors(line);
	line = line.replaceAll("&", "&amp;");
	line = line.replaceAll("<", "&lt;");
	line = line.replaceAll(">", "&gt;");
	Matcher matcher = urlPattern.matcher(line);
	line = matcher.replaceAll("<a href=\"$1\">$1</a>");
	String time = TIME_FORMAT.format(now);
	BufferedWriter bufferedWriter = new BufferedWriter(writer);
	String entry = "<span class=\"irc-date\">[" + time + "]</span> <span class=\"" + color + "\">" + line
		+ "</span><br />";
	bufferedWriter.write(entry);
	bufferedWriter.newLine();
	bufferedWriter.flush();
	bufferedWriter.close();
    }

    Writer getFileWriter(Date now) throws IOException {
	String date = DATE_FORMAT.format(now);
	File file = new File(_outDir, date + ".log");
	return new FileWriter(file, true);
    }

    @Override
    public void onAction(String sender, String login, String hostname, String target, String action) {
	String line = "* " + sender + " " + action;
	append(BRICK, line);
    }

    @Override
    public void onJoin(String channel, String sender, String login, String hostname) {
	String line = "* " + sender + " (" + login + "@" + hostname + ") has joined " + channel;
	append(GREEN, line);
	if (sender.equals(getNick())) {
	    sendNotice(channel, _joinMessage);
	} else {
	    sendNotice(sender, _joinMessage);
	}
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
	String line = "<" + sender + "> " + message;
	append(BLACK, line);
	message = message.toLowerCase();
	if (message.startsWith(getNick().toLowerCase()) && message.indexOf("help") > 0) {
	    sendMessage(channel, _joinMessage);
	}
    }

    @Override
    public void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
	String line = sourceNick + " sets mode " + mode;
	append(GREEN, "* " + line);
    }

    @Override
    public void onNickChange(String oldNick, String login, String hostname, String newNick) {
	append(GREEN, "* " + oldNick + " is now known as " + newNick);
    }

    @Override
    public void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
	String line = "-" + sourceNick + "- " + notice;
	append(BROWN, line);
    }

    @Override
    public void onPart(String channel, String sender, String login, String hostname) {
	String line = "* " + sender + " (" + login + "@" + hostname + ") has left " + channel;
	append(GREEN, line);
    }

    @Override
    public void onPing(String sourceNick, String sourceLogin, String sourceHostname, String target, String pingValue) {
	String line = "[" + sourceNick + " PING]";
	append(RED, line);
    }

    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message) {
	String line = "<- *" + sender + "* " + message;
	append(BLACK, line);
    }

    @Override
    public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
	String line = "* " + sourceNick + " (" + sourceLogin + "@" + sourceHostname + ") Quit (" + reason + ")";
	append(NAVY, line);
    }

    @Override
    public void onTime(String sourceNick, String sourceLogin, String sourceHostname, String target) {
	String line = "[" + sourceNick + " TIME]";
	append(RED, line);
    }

    @Override
    public void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
	String line;
	if (changed) {
	    line = "* " + setBy + " changes topic to '" + topic + "'";
	    append(GREEN, line);
	} else {
	    line = "* Topic is '" + topic + "'";
	    append(GREEN, line);
	    line = "* Set by " + setBy + " on " + new Date(date);
	    append(GREEN, line);
	}
    }

    @Override
    public void onVersion(String sourceNick, String sourceLogin, String sourceHostname, String target) {
	String line = "[" + sourceNick + " VERSION]";
	append(RED, line);
    }

    @Override
    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname,
	    String recipientNick, String reason) {
	String line = "* " + recipientNick + " was kicked from " + channel + " by " + kickerNick;
	append(GREEN, line);
	if (recipientNick.equalsIgnoreCase(getNick())) {
	    joinChannel(channel);
	}
    }

    @Override
    public void onDisconnect() {
	String line = "* " + getName() + " disconnected - trying to reconnect...";
	append(NAVY, line);
	while (!isConnected()) {
	    try {
		reconnect();
	    } catch (Exception e) {
		e.printStackTrace();
		try {
		    Thread.sleep(10000);
		} catch (Exception anye) {
		    // Do nothing.
		}
	    }
	}
    }

    public static void copy(File source, File target) throws IOException {
	BufferedInputStream input = new BufferedInputStream(new FileInputStream(source));
	BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(target));
	int bytesRead = 0;
	byte[] buffer = new byte[1024];
	while ((bytesRead = input.read(buffer, 0, buffer.length)) != -1) {
	    output.write(buffer, 0, bytesRead);
	}
	output.flush();
	output.close();
	input.close();
    }

    public final synchronized void connect(String server, String pass)
	    throws IOException, IrcException, NickAlreadyInUseException {
	connect(server, DEFAULT_PORT, pass);
    }

}