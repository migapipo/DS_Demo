//Revised by Yige Wen
//Server's connection to client or server
//Storing socket and data stream
//Each connection takes place a thread

package activitystreamer.server;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import activitystreamer.util.Settings;


public class Connection extends Thread {
	private static final Logger log = LogManager.getLogger();
	private DataInputStream in;
	private DataOutputStream out;
	private BufferedReader inreader;
	private PrintWriter outwriter;
	private boolean open = false;
	private Socket socket;
	private boolean term=false;
	
	Connection(Socket socket) throws IOException{
		in = new DataInputStream(socket.getInputStream());
	    out = new DataOutputStream(socket.getOutputStream());
	    inreader = new BufferedReader( new InputStreamReader(in));
	    outwriter = new PrintWriter(out, true);
	    this.socket = socket;
	    open = true;
	    start();
	}
	
	@SuppressWarnings("unchecked")
	public void run(){
		try {
			if(Settings.getRemoteHostname() != null){//initiate Authenticate
				Settings.setRemoteHostname(null);
				JSONObject outMsg = new JSONObject();
				outMsg.put("command","AUTHENTICATE");
				outMsg.put("secret",Settings.getSecret());
				writeMsg(outMsg.toJSONString());
				LocalserverStorage.getInstance().getAuthServers().add(socket.getRemoteSocketAddress().toString());
//				System.out.println("initiate link from local sever to remote server");
			}
			
			String data;
			while(!term){//listen from linked client or server
				if((data = inreader.readLine())!=null){
//					System.out.println("reveive: "+data);
					term=Control.getInstance().process(this,data);
				}
			}
			log.debug("connection closed to "+Settings.socketAddress(socket));
			Control.getInstance().connectionClosed(this);
			in.close();
			
		} catch (IOException e) {
			log.error("connection "+Settings.socketAddress(socket)+" closed with exception: "+e);
			Control.getInstance().connectionClosed(this);
		}
		open=false;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public boolean isOpen() {
		return open;
	}
	
	/*
	 * returns true if the message was written, otherwise false
	 */
	public boolean writeMsg(String msg) {
		if(open){
			outwriter.println(msg);
			outwriter.flush();
			return true;	
		}
		return false;
	}
	
	public void closeCon(){
		if(open){
			log.info("closing connection "+Settings.socketAddress(socket));
			try {
				term=true;
				inreader.close();
				out.close();
			} catch (IOException e) {
				// already closed?
				log.error("received exception closing the connection "+Settings.socketAddress(socket)+": "+e);
			}
		}
	}
	
	public void setTerm(boolean x){
		this.term = x;
	}
	
}
