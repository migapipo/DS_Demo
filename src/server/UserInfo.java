//Written by Yige Wen
//Store users' name and password

package activitystreamer.server;

public class UserInfo {
	private String name;
	private String secret;
	
	public UserInfo(String name,String secret){
		this.name = name;
		this.secret = secret;
	}
	
	public UserInfo(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	public String getSecret(){
		return secret;
	}
	
	public boolean equals(UserInfo x){
		return this.name.equals(x.getName())&&this.secret.equals(x.getSecret());
	}
}
