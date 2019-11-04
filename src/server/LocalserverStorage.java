//Writen by Yige Wen
//Local storage memory
//storing users' name and password
//storing logged-in users' name and (IP addr, port)
//storing authenticated or directly linked servers' (IP addr, port)
//storing all other servers' (id,IP addr, port, load)
//storing local server's (id,IP addr, port, load)
//storing username which is in process of regeistering

package activitystreamer.server;

import java.net.Socket;
import java.util.ArrayList;

import javax.imageio.spi.RegisterableService;

import org.json.simple.JSONObject;

import activitystreamer.util.Settings;

public class LocalserverStorage {
	private static ArrayList<UserInfo> users = new ArrayList<UserInfo>();//store registered user info
	private static ArrayList<LinkUsers> authUsers = new ArrayList<LinkUsers>();//store authenticated user info
	private static ArrayList<String> authServers = new ArrayList<String>();//store authenticated servers info
	private static ArrayList<ServerInfo> servers = new ArrayList<ServerInfo>();//store all servers info
	private static ArrayList<Register> registers = new ArrayList<Register>();
			
	private static ServerInfo localInfo = new ServerInfo("ID9527",Settings.getLocalHostname(),
			                                             Settings.getLocalPort(),0);
	
	
//	public static final String secret = Settings.nextSecret();
	public static final String secret = "123";
	protected static LocalserverStorage local = null;
	
	public static LocalserverStorage getInstance(){
		if(local==null){
			local=new LocalserverStorage();
			users.add(new UserInfo("dasima", "123"));
//			System.out.println("Localhost secret is "+secret);
		}
		return local;
	}
	
//----------------------------------------------------------------------------------
	public ArrayList<Register> getRegister(){
		return new ArrayList<Register>(registers);
	}
	
	public void addRegister(Register r){
		registers.add(r);
	}
	
	public void removeRegister(Register r){
		registers.remove(r);
	}
	
	public void addUsers(Register r){
		users.add(new UserInfo(r.getUsername(), r.getSecret()));
	}
	
	public void removeUsers(Register r){
		users.remove(new UserInfo(r.getUsername(), r.getSecret()));
	}
	
	public ServerInfo getLocalInfo() {
		return localInfo;
	}
	
	public ArrayList<ServerInfo> getServers() {
		return servers;
	}
	
	public ArrayList<String> getAuthServers(){
		return authServers;
	}
	
	public ArrayList<UserInfo> getUsers() {
		return users;
	}
	
	public ArrayList<LinkUsers> getAuthUsers(){
		return authUsers;
	}
	
//----------------------------------------------------------------------------------
	
	public static boolean isUser(String name, String password){
		UserInfo temp = new UserInfo(name, password);
		for(UserInfo x:LocalserverStorage.users){
			if(x.equals(temp))return true;
		}
		return false;
	}
	
	public static boolean hasLogin(LinkUsers x){
		for(LinkUsers m:authUsers){
			if(m.equals(x)){
				return true;
			}
		}
		return false;
	}
	
	public static ServerInfo redirectTo(){
		for(int i=0;i<servers.size();i++){
			if((localInfo.getLoad()-servers.get(i).getLoad()) >= 2){
				return servers.get(i);
			}
		}
		return localInfo;
	}
	
	public static void updateServerInfo (JSONObject obj){
		String id = (String) obj.get("id");
		String hostname = (String) obj.get("hostname");
		int port = Integer.parseInt((obj.get("port").toString()));
		int load = Integer.parseInt((obj.get("load").toString()));
		boolean flag = true;//is already existed
		for(ServerInfo x:servers){
			if(id.equals(x.getID())){
				x.setHost(hostname);
				x.setPort(port);
				x.setLoad(load);
				flag = false;
				break;
			}
		}
		if(flag){
			servers.add(new ServerInfo(obj));
		}
	}
	
	public static boolean isExistedAuthUser(LinkUsers user){
		for(LinkUsers x:authUsers){
			if (x.equals(user)){
				return true;
			}
		}
		return false;
	}
	
	public static void removeAuthUser(Socket s){
		for(int i =0;i<authUsers.size();i++){
			if (authUsers.get(i).getAddr().equals(s.getRemoteSocketAddress().toString())){
				authUsers.remove(i);
				break;
			}
		}
	}
	
	public static void updateLoad() {
		localInfo.setLoad(Control.getLoad());
	}
	
	public static boolean isValid(JSONObject obj){
		String command = (String) obj.get("command");
		if(command.equals("AUTHENTICATE")){
			if(obj.get("secret")!=null){
				return true;
			}
		}
		else if(command.equals("LOGIN")){
			if(obj.get("username").toString().equals("anonymous")){
				if(obj.toString().indexOf("secret") == -1){
					return true;
				}
			}
			else if(obj.get("username")!=null && obj.get("secret")!=null){
				return true;
			}
		}
		else if(command.equals("ACTIVITY_MESSAGE") && obj.get("activity") != null){
			if(obj.get("username").toString().equals("anonymous")){
				if(obj.toString().indexOf("secret") == -1 ){
					return true;
				}
			}
			else if(obj.get("username")!=null && obj.get("secret")!=null){
				return true;
			}
		}
		else if(command.equals("SERVER_ANNOUNCE")){
			if(obj.get("id")!=null && obj.get("load")!=null && 
					obj.get("hostname")!=null && obj.get("port")!=null){
				return true;
			}
		}
		else if(command.equals("ACTIVITY_BROADCAST")){
			if(obj.get("activity")!=null){
				return true;
			}
		}
//		else if(command.equals("REGISTER")){
//			if(obj.get("secret")!=null && obj.get("username")!=null){
//				return true;
//			}
//		}
//		else if(command.equals("LOCK_REQUEST")){
//			if(obj.get("secret")!=null && obj.get("username")!=null){
//				return true;
//			}
//		}
//		else if(command.equals("LOCK_DENIED")){
//			if(obj.get("secret")!=null && obj.get("username")!=null){
//				return true;
//			}
//		}
//		else if(command.equals("LOCK_ALLOWED")){
//			if(obj.get("secret")!=null && obj.get("username")!=null){
//				return true;
//			}
//		}
		return false;
	}
}
