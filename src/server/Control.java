//Revised by Yige Wen and Yuankai Li
//Initiating server: listen thread; local storage;
//     incoming connection when accept socket request;
//     outcoming conneciton when set remote host
//Including logic to reply request;
//     Periodically broadcast server_announce 

package activitystreamer.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.util.Settings;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ArrayList<Connection> connections;
	private static boolean term=false;
	private static Listener listener;
	
	protected static Control control = null;
	
	public static Control getInstance() {
		if(control==null){
			control=new Control();
		} 
		return control;
	}
	
	public Control() {
		// initialize the connections array
		connections = new ArrayList<Connection>();
		// start a listener
		try {
			listener = new Listener();
			LocalserverStorage.getInstance();
			if(Settings.getRemoteHostname() != null){
				initiateConnection();
			}
			start();
		} catch (IOException e1) {
			log.fatal("failed to startup a listening thread: "+e1);
			System.exit(-1);
		}	
	}
	
	public void initiateConnection(){
		// make a connection to another server if remote hostname is supplied
		if(Settings.getRemoteHostname()!=null){
			try {
				outgoingConnection(new Socket(Settings.getRemoteHostname(),Settings.getRemotePort()));
			} catch (IOException e) {
				log.error("failed to make connection to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort()+" :"+e);
				System.exit(-1);
			}
		}
	}
	
	/*
	 * Processing incoming messages from the connection.
	 * Return true if the connection should close.
	 */
	@SuppressWarnings("unchecked")
	public synchronized boolean process(Connection con,String msg){
	    JSONParser parser = new JSONParser();
	    JSONObject obj = new JSONObject();
	    try {
			obj = (JSONObject) parser.parse(msg);
			String command = (String) obj.get("command");
			
			if(command.equals("LOGIN")){
				if(LocalserverStorage.isValid(obj)){
					if(isRegist(obj.get("username").toString())){
						if(isLoginSucc(obj)){//login_succ
							String name = (String)obj.get("username");
							JSONObject loginSuccess = new JSONObject();
							loginSuccess.put("command", "LOGIN_SUCCESS");
							loginSuccess.put("info", "logged in as user "+name);
							con.writeMsg(loginSuccess.toJSONString());
							String reDirectTo = LocalserverStorage.redirectTo().getHost();
							int reDirectPort = LocalserverStorage.redirectTo().getPort();
							if(Settings.getLocalHostname().equals(reDirectTo) && Settings.getLocalPort()==reDirectPort){//login,add user
								LinkUsers temp = new LinkUsers(name, con.getSocket());
								if(!LocalserverStorage.isExistedAuthUser(temp)){
									LocalserverStorage.getInstance().getAuthUsers().add(temp);
								}
								return false;
							}
							else{//need redirect
								JSONObject redirect = new JSONObject();
								redirect.put("command", "REDIRECT");
								redirect.put("hostname", reDirectTo);
								redirect.put("port", reDirectPort);
								con.writeMsg(redirect.toJSONString());
								return true;
							}
						}
						else{//login_fail
							JSONObject loginFail = new JSONObject();
							loginFail.put("command", "LOGIN_FAILED");
							loginFail.put("info", "attempt to login with wrong secret");
							con.writeMsg(loginFail.toJSONString());
							return true;
						}
					}
					else{
						JSONObject loginFail = new JSONObject();
						loginFail.put("command", "LOGIN_FAILED");
						loginFail.put("info",obj.get("username").toString()+" is not regeistered");
						con.writeMsg(loginFail.toJSONString());
						return true;
					}
				}
				else{
					String info = "the received message contain invalid command";
					con.writeMsg(invalidMsg(info).toJSONString());
					return true;
				}
			}
			else if(command.equals("LOGOUT")){
				LocalserverStorage.removeAuthUser(con.getSocket());
				return true;
			}
			else if(command.equals("SERVER_ANNOUNCE")){
				if(LocalserverStorage.isValid(obj)){
					String rAddr = con.getSocket().getRemoteSocketAddress().toString();
					if(LocalserverStorage.getInstance().getAuthServers().contains(rAddr)){//update 
						LocalserverStorage.updateServerInfo(obj);
						Broadcast.serverBroadcast(obj, con.getSocket(), false);
						//广播给S,不包括来源
						return false;
					}
					else{//unauth_server_ann
						String info = "the received message is from unauthenticated server";
						con.writeMsg(invalidMsg(info).toJSONString());
						return true;
					}
				}
				else{
					String info = "the received message did not contain a command";
					con.writeMsg(invalidMsg(info).toJSONString());
					return true;
				}
			}
			else if(command.equals("AUTHENTICATE")){
				if(LocalserverStorage.isValid(obj)){
					String rAddr = con.getSocket().getRemoteSocketAddress().toString();
					if(!LocalserverStorage.getInstance().getAuthServers().contains(rAddr)){
						String secret = (String)obj.get("secret");
						String rhAddr = con.getSocket().getRemoteSocketAddress().toString();
						if(secret.equals(LocalserverStorage.secret)){//authen_succ,add addr into linkservers
							LocalserverStorage.getInstance().getAuthServers().add(rhAddr);
							return false;
						}
						else{//authen_fail
							JSONObject authenFail = new JSONObject();
							authenFail.put("command","AUTHENTICATION_FAIL");
							authenFail.put("info","the supplied secret is incorrect: "+secret);
							con.writeMsg(authenFail.toJSONString());
							LocalserverStorage.getInstance().getAuthServers().remove(rhAddr);
							return true;
						}
					}
					else{//double authen
						String info = "the server has already been authenticated";
						con.writeMsg(invalidMsg(info).toJSONString());
						return true;
					}
				}
				else{
					String info = "the received message did not contain a command";
					con.writeMsg(invalidMsg(info).toJSONString());
					return true;
				}
			}
			else if(command.equals("AUTHENTICATE_FAIL")){
				String rhAddr = con.getSocket().getRemoteSocketAddress().toString();
				LocalserverStorage.getInstance().getAuthServers().remove(rhAddr);
				log.info("received an AUTHENTICATE_FAIL");
				return true;
			}
			else if(command.equals("INVALID_MESSAGE")){
				log.info("received an INVALID_MESSAGE");
				return true;
			}
			else if(command.equals("ACTIVITY_MESSAGE")){
				if(LocalserverStorage.isValid(obj)){
					if(isLoginSucc(obj)){
						LinkUsers temp = new LinkUsers(obj.get("username").toString(), con.getSocket());
						if(LocalserverStorage.hasLogin(temp)){//broadcast
							JSONObject act = new JSONObject();
							act = (JSONObject) obj.get("activity");
							act.put("authencated_user", obj.get("username"));
							JSONObject actMsg = new JSONObject();
							actMsg.put("command","ACTIVITY_BROADCAST");
							actMsg.put("activity",act);
							Broadcast.allBroadcast(actMsg, con.getSocket(), true);
							return false;
						}
						else{//not login yet
							JSONObject authenFail = new JSONObject();
							authenFail.put("command","AUTHENTICATION_FAIL");
							authenFail.put("info","the user is not login yet");
							con.writeMsg(authenFail.toJSONString());
							return true;
						}
					}else{//username or secret wrong
						JSONObject authenFail = new JSONObject();
						authenFail.put("command","AUTHENTICATION_FAIL");
						authenFail.put("info","the username or secret is incorrect");
						con.writeMsg(authenFail.toJSONString());
						return true;
					}
				}
				else{
					String info = "the received message did not contain a command";
					con.writeMsg(invalidMsg(info).toJSONString());
					return true;
				}
			}
			else if(command.equals("ACTIVITY_BROADCAST")){
				if(LocalserverStorage.isValid(obj)){
					String rAddr = con.getSocket().getRemoteSocketAddress().toString();
					if(LocalserverStorage.getInstance().getAuthServers().contains(rAddr)){//repeater
						Broadcast.allBroadcast(obj, con.getSocket(), false);
						return false;
					}
					else{//message from unauthenticated server
						String info = "the received message is from unauthenticated server";
						con.writeMsg(invalidMsg(info).toJSONString());
						return true;
					}
				}
				else{
					String info = "the received message did not contain a command";
					con.writeMsg(invalidMsg(info).toJSONString());
					return true;
				}
			}
			else if(command.equals("LOCK_REQUEST")){
				try {
					JSONObject json = (JSONObject) parser.parse(msg);
					Register r = new Register(json, con.getSocket());
					return Lock.lockRequest(r);		
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
			else if(command.equals("LOCK_DENIED")){
				try {
					JSONObject json = (JSONObject) parser.parse(msg);
					Register r = new Register(json, con.getSocket());
					return Lock.lockDenied(r);				
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
			else if(command.equals("LOCK_ALLOWED")){
				try {
					JSONObject json = (JSONObject) parser.parse(msg);
					Register r = new Register(json, con.getSocket());
					return Lock.lockAllowed(r);					
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
			else if(command.equals("REGISTER")){
				try {
					JSONObject json = (JSONObject) parser.parse(msg);
					Register r = new Register(json, con.getSocket());
					return Register.register(r);					
				}catch(Exception e) {
					e.printStackTrace();
				}
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	/*
	 * The connection has been closed by the other party.
	 */
	public synchronized void connectionClosed(Connection con){
		if(!term) connections.remove(con);
	}
	
	/*
	 * A new incoming connection has been established, and a reference is returned to it
	 */
	public synchronized Connection incomingConnection(Socket s) throws IOException{
		log.debug("incomming connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		return c;
	}
	
	/*
	 * A new outgoing connection has been established, and a reference is returned to it
	 */
	public synchronized Connection outgoingConnection(Socket s) throws IOException{
		log.debug("outgoing connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		return c;
	}
	
	@Override
	public void run(){
		log.info("using activity interval of "+Settings.getActivityInterval()+" milliseconds");
		while(!term){
			// do something with 5 second intervals in between
			try {
				Thread.sleep(Settings.getActivityInterval());
			} catch (InterruptedException e) {
				log.info("received an interrupt, system is shutting down");
				break;
			}
			if(!term){
				log.debug("doing activity");
				term=doActivity();
			}
		}
		log.info("closing "+connections.size()+" connections");
		// clean up
		for(Connection connection : connections){
			connection.closeCon();
		}
		listener.setTerm(true);
	}
	
	public boolean doActivity(){
		if(LocalserverStorage.getInstance().getAuthServers().size()>0){
			LocalserverStorage.updateLoad();
			JSONObject serverAnn = new JSONObject();
			serverAnn.put("command","SERVER_ANNOUNCE");
			serverAnn.put("id",LocalserverStorage.getInstance().getLocalInfo().getID());
			serverAnn.put("load",LocalserverStorage.getInstance().getLocalInfo().getLoad());
			serverAnn.put("hostname",LocalserverStorage.getInstance().getLocalInfo().getHost());
			serverAnn.put("port",LocalserverStorage.getInstance().getLocalInfo().getPort());
			Broadcast.serverBroadcast(serverAnn,null,true);
		}
		return false;
	}
	
	public final void setTerm(boolean t){
		term=t;
	}
	
	public final ArrayList<Connection> getConnections() {
		return connections;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject invalidMsg(String info){
		JSONObject invalidmsg = new JSONObject();
		invalidmsg.put("command", "INVALID_MESSAGE");
		invalidmsg.put("info", info);
		return invalidmsg;
	}
	
	public static boolean isLoginSucc(JSONObject obj){
		String name = (String)obj.get("username");
		if(name.equals("anonymous"))return true;
		String password = (String)obj.get("secret");
		return LocalserverStorage.isUser(name,password);
	}
	
	public static boolean isRegist(String x){
		if(x.equals("anonymous"))return true;
		for(UserInfo m:LocalserverStorage.getInstance().getUsers()){
			if(x.equals(m.getName()))return true;
		}
		return false;
	}
	
	public static int getLoad(){
		return connections.size();
	}
}
