//Writen by Yige Wen
//Storing users who have login

package activitystreamer.server;

import java.net.Socket;

public class LinkUsers{
	private String name;
	private String addr;
		
	public LinkUsers(String name,Socket s){
		this.name = name;
		this.addr = s.getRemoteSocketAddress().toString();
	}
	
	public String getName(){
		return name;
	}
	
	public String getAddr() {
		return addr;
	}
	
	public boolean equals(LinkUsers x) {
		return this.name.equals(x.getName()) && this.addr.equals(x.getAddr());
	}
}
