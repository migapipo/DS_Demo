//Written by Yige Wen
//Broadcasting message and two models  
//1. to all clients and servers (source or repeater)
//2. only among servers (source or repeater)

package activitystreamer.server;

import java.net.Socket;

import org.json.simple.JSONObject;

import activitystreamer.util.Settings;

public class Broadcast extends Thread{
	
	public static void allBroadcast(JSONObject obj,Socket s,boolean f){//f:true allBroad;f:false not to source
		for(Connection con:Control.getInstance().getConnections()){
			if(f){
				con.writeMsg(obj.toJSONString());
			}
			else{
				if(!con.getSocket().getRemoteSocketAddress().toString().equals(s.getRemoteSocketAddress().toString())){
					con.writeMsg(obj.toJSONString());
				}
			}
		}
	}
	
	public static void serverBroadcast(JSONObject obj,Socket source,boolean f){//f:true allBroad;f:false not to source
		
		for(Connection con:Control.getInstance().getConnections()){
			String conAddr = con.getSocket().getRemoteSocketAddress().toString();
			if(f){
				if(LocalserverStorage.getInstance().getAuthServers().contains(conAddr)){//all linked servers
					con.writeMsg(obj.toJSONString());
				}
			}
			else{
				String sourceAddr = source.getRemoteSocketAddress().toString();
				if(!sourceAddr.equals(conAddr)){//not to source
					if(LocalserverStorage.getInstance().getAuthServers().contains(conAddr)){//not to source
						con.writeMsg(obj.toJSONString());
					}
				}
			}
		}
	}
}
