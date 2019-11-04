//Written by Zijian Zhao
//Initiate client
//Hold two thread: 1. listen from keyboard by GUI
//Get reply from and send request to server

package activitystreamer.client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.*;
import activitystreamer.util.Settings;
public class ClientSkeleton extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ClientSkeleton clientSolution;
	private TextFrame textFrame;
	private boolean term = false;
	Socket socket = null;
	
	public static ClientSkeleton getInstance(){
		if(clientSolution==null){
			clientSolution = new ClientSkeleton();
		}
		return clientSolution;
	}
	public ClientSkeleton(){
		start();
	}
	public void initiateConnection(){
		if(Settings.getRemoteHostname()!=null){
			try {
				socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
			} catch (UnknownHostException e) {
				log.error("UnknownHost: " + e);
				System.exit(-1);
			} catch (IOException e) {
				log.error("Failed to make a connection with " +
						Settings.getRemoteHostname() + ":" +
						Settings.getRemotePort() + " :" + e);
				System.exit(-1);
			}
		}
	}
	public JSONObject sendActivityObject(JSONObject activityObj){
		try {
			String username = Settings.getUsername();
			String secret = Settings.getSecret();
			OutputStream os = socket.getOutputStream();
			PrintWriter pw = new PrintWriter(os);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("command", "ACTIVITY_MESSAGE");
			jsonObject.put("username", username);
			if (!username.equals("anonymous")) {
				jsonObject.put("secret", secret);
			}
			jsonObject.put("activity", activityObj);
			pw.println(jsonObject);
			pw.flush();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public JSONObject getInput() throws IOException, JSONException{
		InputStream is = socket.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = br.readLine();
		is.close();
		br.close();
		JSONObject reply = new JSONObject(line);
		return reply;
	}
	
	public void closeTextFrame(){
		textFrame.setVisible(false);
	}
	
	public void disconnect() throws JSONException{
		try {
			OutputStream os = socket.getOutputStream();
			PrintWriter pw = new PrintWriter(os);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("command", "LOGOUT");
			pw.print(jsonObject);
			pw.flush();
			System.exit(-1);
		} catch (IOException e) {
			log.error("*** The connection has been closed ***");
		}
	}
	
	public void run() {
		initiateConnection();
		try {
			String username = Settings.getUsername();
			String secret = Settings.getSecret();
			OutputStream os = socket.getOutputStream();
			PrintWriter pw = new PrintWriter(os);
			JSONObject jsonObject = new JSONObject();
			if (username.equals("anonymous")){
				jsonObject.put("command", "LOGIN");
			} else if (secret!=null) {
				jsonObject.put("command", "LOGIN");
				jsonObject.put("secret", secret);
			} else {
				jsonObject.put("command", "REGISTER");
				String givenSecret = Settings.nextSecret();
				Settings.setSecret(givenSecret);
				jsonObject.put("secret", givenSecret);
				log.info("*** Your allowcated secret is "+givenSecret+" ***");
			}
			jsonObject.put("username", username);
			pw.println(jsonObject);
			pw.flush();
			InputStream is = socket.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while(!term){
				if((line = br.readLine())!=null){
					System.out.println("received from"+socket.getRemoteSocketAddress()+"JSON"+line);//Used for dubug
					JSONObject reply = new JSONObject(line);
					String command = "";
					if (!reply.isNull("command")) {
						command = reply.getString("command");
					}
					if (command.equals("INVALID_MESSAGE")) {
						String info = reply.getString("info");
						log.info("*** " + info + " ***");
						os.close();
						pw.close();
						is.close();
						br.close();
						disconnect();
						term = true;
						System.exit(-1);
					}
					else if (command.equals("LOGIN_SUCCESS")) {
						String info = reply.getString("info");
						log.info("*** " + info + " ***");
						log.info("*** Opening the GUI ***");
						textFrame = new TextFrame();
						textFrame.setVisible(true);
					}
					else if (command.equals("LOGIN_FAILED")) {
						String info = reply.getString("info");
						log.info("*** " + info + " ***");
						os.close();
						pw.close();
						is.close();
						br.close();
						disconnect();
						term = true;
						System.exit(-1);
					}
					else if (command.equals("REGISTER_SUCCESS")) {
						String info = reply.getString("info");
						log.info("*** " + info + " ***");
						JSONObject login = new JSONObject();
						login.put("command", "LOGIN");
						login.put("username", Settings.getUsername());
						login.put("secret", Settings.getSecret());
						pw.println(login);
						pw.flush();
					}
					else if (command.equals("REGISTER_FAILED")) {
						String info = reply.getString("info");
						log.info("*** " + info + " ***");
						os.close();
						pw.close();
						is.close();
						br.close();
						disconnect();
						term = true;
						System.exit(-1);
					}
					else if (command.equals("REDIRECT")) {
						textFrame.setVisible(false);
						os.close();
						pw.close();
						is.close();
						br.close();
						disconnect();
						if (!reply.isNull("hostname")) {
							String redirectHostname = reply.getString("hostname");
							Settings.setRemoteHostname(redirectHostname);
						}
						if (!reply.isNull("port")) {
							int redirectPort = reply.getInt("port");
							Settings.setRemotePort(redirectPort);
						}
						log.info("*** Redirect: The connection has been changed into a new server " + Settings.getRemoteHostname() + " : "
								+ Settings.getRemotePort() + " ***");
						clientSolution = new ClientSkeleton();
						term = true;
					}
					else if (command.equals("ACTIVITY_BROADCAST")) {
						JSONObject obj = new JSONObject();
						obj.put("command","ACTIVITY_BROADCAST");
						obj.put("activity",reply.get("activity"));
						textFrame.setOutputText(obj);
					}
					else {
						log.info("*** The return message from Server is not readable ***");
						disconnect();
						term = true;
					}
				}
			}	
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}
}
