package LatoClient;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Ascolta_chatroom implements Runnable {

	private static int porta=5000;
	private ConcurrentHashMap<String,Interfaccia_ChatRoom> chatroom_frames;
	// IP del server
	private static String IP="127.0.0.1";
	// porta connessione di controllo del server
	private static int port=5000;
	private String utente;
	private MulticastSocket s;
	
	public Ascolta_chatroom(ConcurrentHashMap<String,Interfaccia_ChatRoom> chatroom_frames, String utente){
		this.chatroom_frames=chatroom_frames;
		this.utente=utente;
	}
	
	@Override
	public void run() {
		try {
			s = new MulticastSocket(porta);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while (true){
			byte buf[] = new byte[2048];
			DatagramPacket pack = new DatagramPacket(buf, buf.length);
			try {
				s.receive(pack);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String nome_gruppo=null;
			String str=new String(buf);
			str=str.trim();
			
			JSONParser parser = new JSONParser();
			Object obj = null;
			try {
				obj = parser.parse(str);
			} catch (org.json.simple.parser.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			JSONObject jsonObject = (JSONObject) obj;
			JSONArray array=  (JSONArray) jsonObject.get("list iscritti");
			nome_gruppo = (String) jsonObject.get("nome_gruppo");
			String creatore = (String) jsonObject.get("creatore");
			String iscritto = (String) jsonObject.get("iscritto");
			String body = (String) jsonObject.get("corpo");
			
			
			ArrayList<String> u = new ArrayList<String>();
			
			if (array != null)
				for (int i=0; i<array.size(); i++){
					JSONObject Jogg = (JSONObject) array.get(i);
					String nome= (String) Jogg.get("nome");
					String stato = (String) Jogg.get("stato");
					
					if (stato.equals("online"))
						u.add("• " + nome);
					else
						u.add("ø " + nome);
				}
			
			if (!chatroom_frames.containsKey(nome_gruppo))
				chatroom_frames.put(nome_gruppo,new Interfaccia_ChatRoom(u,creatore,iscritto,IP, port,utente,nome_gruppo,chatroom_frames, this));
				
				
			chatroom_frames.get(nome_gruppo).getTextArea().append( body + "\n");
		}
	}
	
	public void Unisci_algruppo(String IP){
		System.out.println("join " + IP);
		try {
			s.joinGroup(InetAddress.getByName(IP));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void lascia_gruppo(String IP){
		System.out.println("lascia " + IP);
		try {
			s.leaveGroup(InetAddress.getByName(IP));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}