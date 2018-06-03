package LatoServer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



/* Thread che si occupa di mandare un pacchetto UDP multicast utilizzando la coppia
 * <IP,porta> relativi alla chatroom, la porta è fissata (5000)  */
public class Task_chatroom implements Runnable {
	
	private String line;
	private ConcurrentHashMap<String,ChatRoom> chatroom;
	
	
	public Task_chatroom(String line,ConcurrentHashMap<String,ChatRoom> chatroom){
		this.line=line;
		this.chatroom=chatroom;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(line);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		
		System.out.println(jsonObject);
		
		JSONObject messaggio = (JSONObject) jsonObject.get("messaggio chatroom");
		String mittente= (String) messaggio.get("mittente");
		String destinatario= (String) messaggio.get("destinatario");
		String corpo= (String) messaggio.get("body");
		
		
		ChatRoom c=chatroom.get(destinatario);
		
		JSONObject array = new JSONObject();
		JSONArray iscritti = new JSONArray();
		
		String creatore;
		String iscritto;
		
		if (c!=null){
			for (Utente u: c.partecipanti()){
				JSONObject ogg= new JSONObject ();
				ogg.put("nome", u.getnome());
				ogg.put("stato", u.getstato());
				iscritti.add(ogg);
			}
			
			array.put("list iscritti",iscritti);
			
			
			creatore=c.getCreatore().getnome();
			
			
			if (c.partecipante(mittente))
				iscritto=new String("si");
			else
				iscritto=new String("no");
				
			array.put("nome_gruppo", c.getNome());
			array.put("creatore", creatore);
			array.put("iscritto", iscritto);
			array.put("corpo", corpo);
			
			String str=array.toJSONString();
			
			byte b[] = str.getBytes();
			
			MulticastSocket s = null;
			try {
				s = new MulticastSocket();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			String IP=chatroom.get(destinatario).getIP();
			 
			DatagramPacket pack = null;
			try {
				pack = new DatagramPacket(b, b.length,InetAddress.getByName(IP), 5000);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				s.setTimeToLive(1);
				s.send(pack);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			s.close();
		}			
	}
}
