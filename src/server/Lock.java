//Writen by Yuankai Li
//Process lock message broadcasted from other servers
//an repeat the message
//Search local storage to give reply

package activitystreamer.server;

import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.net.Socket;
import org.json.simple.JSONObject;

public class Lock {
	
	private static ArrayList<UserInfo> localUser = LocalserverStorage.getInstance().getUsers();
	
	public static boolean lockRequest(Register r) {
		String username = r.getUsername();
		String secret = r.getSecret();
		Socket socket = r.getSocket();
		for(UserInfo u : localUser) {
			if(username.equals(u.getName())) {
				try {
					JSONObject reply = new JSONObject();
					reply.put("command", "LOCK_DENIED");
					reply.put("username", username);
					reply.put("secret", secret);
					/*DataOutputStream out = new DataOutputStream (r.getSocket().getOutputStream());
					byte[] replyByte = reply.toString().getBytes();
					out.write(replyByte);
					out.flush();*/
					Broadcast.serverBroadcast(reply, socket, true);
					return false;
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		try {
			LocalserverStorage.getInstance().addUsers(r);
			JSONObject relay = new JSONObject();
			relay.put("command", "LOCK_REQUEST");
			relay.put("username", username);
			relay.put("secret", secret);
			Broadcast.serverBroadcast(relay, socket, false);
			JSONObject reply = new JSONObject();
			reply.put("command", "LOCK_ALLOWED");
			reply.put("username", username);
			reply.put("secret", secret);
			/*DataOutputStream out = new DataOutputStream (r.getSocket().getOutputStream());
			byte[] replyByte = reply.toString().getBytes();
			out.write(replyByte);
			out.flush();*/
			Broadcast.serverBroadcast(reply, socket, true);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean lockDenied(Register r) {
		ArrayList<Register> rl = LocalserverStorage.getInstance().getRegister();
		for(Register a : rl) {
			if(a.getUsername().equals(r.getUsername())) {
				try {
					JSONObject reply = new JSONObject();
					reply.put("command", "REGISTER_FAILED");
					reply.put("info", a.getUsername() + " is already registered with the system");
					DataOutputStream out = new DataOutputStream (a.getSocket().getOutputStream());
//					byte[] replyByte = reply.toString().getBytes();
//					out.write(replyByte);
					PrintWriter outwriter = new PrintWriter(out, true);
					outwriter.println(reply.toJSONString());
					out.flush();
					for(Connection c:Control.getInstance().getConnections()){
//						String raddr = r.getSocket().getRemoteSocketAddress().toString();
//						String caddr = c.getSocket().getRemoteSocketAddress().toString();
						if(r.getSocket().equals(c.getSocket())){
							c.setTerm(true);
						}
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
			LocalserverStorage.getInstance().removeRegister(a);
			return true;
		}
		JSONObject relay = new JSONObject();
		relay.put("command", "LOCK_DENIED");
		relay.put("username", r.getUsername());
		relay.put("secret", r.getSecret());
		Broadcast.serverBroadcast(relay, r.getSocket(), false);
		return false;
	}
	
	public static boolean lockAllowed(Register r) {
		ArrayList<Register> rl = LocalserverStorage.getInstance().getRegister();
		for(Register a : rl) {
			if(a.getUsername().equals(r.getUsername())) {
				a.addAllows();
				if(a.getAllows() == LocalserverStorage.getInstance().getServers().size()) {
					try {
						JSONObject reply = new JSONObject();
						reply.put("command", "REGISTER_SUCCESS");
						reply.put("info", "register success for " + a.getUsername());
						DataOutputStream out = new DataOutputStream (a.getSocket().getOutputStream());
//						byte[] replyByte = reply.toString().getBytes();
//						out.write(replyByte);
						PrintWriter outwriter = new PrintWriter(out, true);
						outwriter.println(reply.toJSONString());
						out.flush();
					}catch(Exception e) {
						e.printStackTrace();
					}
					LocalserverStorage.getInstance().addUsers(a);
					return false;
				}
			}
		}
		JSONObject relay = new JSONObject();
		relay.put("command", "LOCK_ALLOWED");
		relay.put("username", r.getUsername());
		relay.put("secret", r.getSecret());
		Broadcast.serverBroadcast(relay, r.getSocket(), false);
		return false; 
	}
	
}
