//Writen by Yuankai Li
//Hold the logic and mechanism of register
//Reply the register command of client

package activitystreamer.server;

import java.util.ArrayList;
import org.json.simple.*;
import java.net.Socket;
import java.io.*;

public class Register{
	
	private JSONObject obj;
	private String username;
	private String secret;
	private Socket socket;
	private int allows;
	
	public Register(JSONObject obj, Socket socket) {	
		try {
			this.obj = obj;
			this.username = (String)obj.get("username");
			this.secret = (String)obj.get("secret");
			this.socket = socket;
			this.allows = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean register(Register r) {
		try {
			if(r.getUsername() == null || r.getSecret() == null) {
				JSONObject reply = new JSONObject();
				reply.put("command", "INVALID_MESSAGE");
				reply.put("info", "incorrect message");
				DataOutputStream out = new DataOutputStream (r.getSocket().getOutputStream());
				PrintWriter outwriter = new PrintWriter(out, true);
				outwriter.println(reply.toJSONString());
				out.flush();
				for(Connection c:Control.getInstance().getConnections()){
					String raddr = r.getSocket().getRemoteSocketAddress().toString();
					String caddr = c.getSocket().getRemoteSocketAddress().toString();
					if(r.getSocket().equals(c.getSocket())){
						c.setTerm(true);
					}
				}
				return true;
			}else {
				for(UserInfo u : LocalserverStorage.getInstance().getUsers()) {
					if(r.username.equals(u.getName())) {
						for(LinkUsers v : LocalserverStorage.getInstance().getAuthUsers()) {
							if(r.username.equals(v.getName())) {
								JSONObject reply = new JSONObject();
								reply.put("command", "INVALID_MESSAGE");
								reply.put("info", "user already logged in");
								DataOutputStream out = new DataOutputStream (r.socket.getOutputStream());
								PrintWriter outwriter = new PrintWriter(out, true);
								outwriter.println(reply.toJSONString());
								out.flush();
								for(Connection c:Control.getInstance().getConnections()){
									String raddr = r.getSocket().getRemoteSocketAddress().toString();
									String caddr = c.getSocket().getRemoteSocketAddress().toString();
									if(r.getSocket().equals(c.getSocket())){
										c.setTerm(true);
									}
								}
								return true;
							}
						}
						JSONObject reply = new JSONObject();
						reply.put("command", "REGISTER_FAILED");
						reply.put("info", r.username + " is already registered with the system");
						DataOutputStream out = new DataOutputStream (r.socket.getOutputStream());
						PrintWriter outwriter = new PrintWriter(out, true);
						outwriter.println(reply.toJSONString());
						out.flush();
						for(Connection c:Control.getInstance().getConnections()){
							String raddr = r.getSocket().getRemoteSocketAddress().toString();
							String caddr = c.getSocket().getRemoteSocketAddress().toString();
							if(r.getSocket().equals(c.getSocket())){
								c.setTerm(true);
							}
						}
						return true;
					}
				}
				for(Register a : LocalserverStorage.getInstance().getRegister()) {
					if(r.username.equals(a.getUsername())) {
						JSONObject reply = new JSONObject();
						reply.put("command", "REGISTER_FAILED");
						reply.put("info", r.username + " is already registered with the system");
						DataOutputStream out = new DataOutputStream (r.socket.getOutputStream());
						PrintWriter outwriter = new PrintWriter(out, true);
						outwriter.println(reply.toJSONString());
						out.flush();
						for(Connection c:Control.getInstance().getConnections()){
							String raddr = r.getSocket().getRemoteSocketAddress().toString();
							String caddr = c.getSocket().getRemoteSocketAddress().toString();
							if(r.getSocket().equals(c.getSocket())){
								c.setTerm(true);
							}
						}
						return true;
					}
				}
				if(LocalserverStorage.getInstance().getServers().size() == 0) {
					LocalserverStorage.getInstance().addUsers(r);
					JSONObject reply = new JSONObject();
					reply.put("command", "REGISTER_SUCCESS");
					reply.put("info", "register success for " + r.getUsername());
					DataOutputStream out = new DataOutputStream (r.getSocket().getOutputStream());
					PrintWriter outwriter = new PrintWriter(out, true);
					outwriter.println(reply.toJSONString());
					out.flush();
					return false;
				}else {
					LocalserverStorage.getInstance().addRegister(r);
					JSONObject request = new JSONObject();
					request.put("command", "LOCK_REQUEST");
					request.put("username", r.getUsername());
					request.put("secret", r.getSecret());
					Broadcast.serverBroadcast(request, r.getSocket(), true);
					return false;
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public JSONObject getObject() {
		return this.obj;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public String getSecret() {
		return this.secret;
	}
	
	public Socket getSocket() {
		return this.socket;
	}
	
	public int getAllows() {
		return this.allows;
	}
	
	public void addAllows() {
		this.allows ++;
	}
	
}
