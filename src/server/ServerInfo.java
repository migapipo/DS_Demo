//Writern by Yige Wen
//Store server_announce from other servers

package activitystreamer.server;

import org.json.simple.JSONObject;

public class ServerInfo {
	private String ID;
	private String host;
	private int port;
	private int load;
	
	public ServerInfo(String ID, String host, int port,int load){
		this.ID = ID;
		this.host = host;
		this.port = port;
		this.load = load;
	}
	
	public ServerInfo(JSONObject obj){
		this.ID = (String) obj.get("id");
		this.host = (String) obj.get("hostname");
		this.port =  Integer.parseInt((obj.get("port").toString()));
		this.load =  Integer.parseInt((obj.get("load").toString()));
	}
	
	public String getID(){
		return ID;
	}
	
	public void setID(String ID){
		this.ID = ID;
	}
	
	public String getHost(){
		return host;
	}
	
	public void setHost(String host){
		this.host = host;
	}
	
	public int getPort(){
		return port;
	}
	
	public void setPort(int port){
		this.port = port;
	}
	
	public int getLoad(){
		return load;
	}
	
	public void setLoad(int load){
		this.load = load;
	}
	
	public boolean equals(ServerInfo obj) {
		return this.ID.equals(obj.getID());
	}

}
