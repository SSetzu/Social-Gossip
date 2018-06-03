package LatoServer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/* Thread connessione di controllo, riceve il socket, svolge l'operazione richiesta e chiude il socket */
public class Service implements Runnable {
	
	/* socket connessione di controllo */
	private Socket socket;
	
	/* oggetto RMI */
	private ServerImpl server;
	
	/* prefisso IP multicast */
	private static String IP = "239.1.1.";
	/* postfisso IP multicast */
	private static int fine_ip=0;
	
	/* hashmap concorrente che contiene gli utenti e le chatroom */
	private ConcurrentHashMap<String,Utente> utenti;
	private ConcurrentHashMap<String,ChatRoom> chatroom;
	
	public Service(Socket socket, ConcurrentHashMap<String,Utente> utenti, ConcurrentHashMap<String,ChatRoom> chatroom, ServerImpl server){
		this.socket=socket;
		this.utenti=utenti;
		this.server=server;
		this.chatroom=chatroom;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		BufferedReader reader;
		
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		
			
				String line = reader.readLine();
				
				if (line==null) return;
				
				JSONParser parser = new JSONParser();
				Object obj = parser.parse(line);
				JSONObject jsonObject = (JSONObject) obj;
				
				
				if (jsonObject.containsKey("login")){
					JSONObject login = (JSONObject) jsonObject.get("login");
					String utente = (String) login.get("utente");
					String password = (String) login.get("password");
					String indirizzo = (String) login.get("indirizzo");
					String p_mess = (String) login.get("porta messaggi");
					String p_file = (String) login.get("porta file");
					String p_online = (String) login.get("porta online");
					
					int porta_messaggi=(new Integer(p_mess)).intValue();
					int porta_file=(new Integer(p_file)).intValue();
					int porta_online=(new Integer(p_online)).intValue();
					
					if (utenti.containsKey(utente)){
						
							Utente u=utenti.get(utente);
						
							if (u.getnome().equals(utente) && u.getpassword().equals(password)){
							
								u.SetOnline();
								u.setIP(indirizzo);
								
								writer.write("Accesso Consentito");
								writer.newLine(); 
								writer.flush();
																
								u.setPorta_messaggi(porta_messaggi);
								u.setPorta_file(porta_file);
								u.setPorta_online(porta_online);
								
								JSONObject messaggio = new JSONObject();
								JSONArray array = new JSONArray();
								
								for (ChatRoom c: u.getGroups())
								{
									JSONObject o = new JSONObject();
									o.put("IP chat", c.getIP());
									array.add(o);
								}
								
								messaggio.put("tue chatroom", array);
								
								writer.write(messaggio.toJSONString());
								writer.newLine(); 
								writer.flush(); 
								
							}
							else
							{
								writer.write("Accesso Negato");
								writer.newLine(); 
								writer.flush();
							}
					}
					else
					{
						writer.write("Accesso Negato");
						writer.newLine(); 
						writer.flush();
					}
					
				}
				else if (jsonObject.containsKey("registrazione")){
					JSONObject reg = (JSONObject) jsonObject.get("registrazione");
					String utente = (String) reg.get("utente");
					String password = (String) reg.get("password");
					String lingua = (String) reg.get("lingua");
					
					String s=null;
					
					if (utenti.containsKey(utente))
						s=new String("Registrazione Negata");
					else
					{
						s=new String("Registrazione Consentita");
						utenti.put(utente,new Utente(utente, password, lingua));
					}
					
					writer.write(s);
					writer.newLine(); 
					writer.flush();	
					
				}
				else if (jsonObject.containsKey("ricerca")){
					String utente = (String) jsonObject.get("ricerca");
					int num_utenti=0;
					
					for (Utente u : utenti.values())
						if (utente.length()<=u.getnome().length())
							if ((u.getnome().substring(0, utente.length())).equals(utente))
								num_utenti++;
							
					writer.write((new Integer(num_utenti)).toString());
					writer.newLine(); 
					writer.flush();
						
					
					for (Utente u : utenti.values())
						if (utente.length()<=u.getnome().length()){
							if ((u.getnome().substring(0, utente.length())).equals(utente)){
								writer.write(u.getnome());
								writer.newLine(); 
								writer.flush();
								}
						}
				}
				else if (jsonObject.containsKey("amicizia")){
					JSONObject richiesta = (JSONObject) jsonObject.get("amicizia");
					String mittente = (String) richiesta.get("mittente");
					String destinatario = (String) richiesta.get("destinatario");
					Utente mitt = utenti.get(mittente);
					Utente dest = utenti.get(destinatario);
					
					if (mitt==dest){
						writer.write("Non puoi richiedere l'amicizia a te stesso, sei una persona molto sola!!");
						writer.newLine(); 
						writer.flush();
					}						
					else
					{
						Boolean flag=false;
						for (Utente u: mitt.listfriend())
							if (u==dest){
									writer.write("Sei già amico dell'utente" + " " + destinatario);
									writer.newLine(); 
									writer.flush();
									flag=true;
							}
						
						if (!flag){
							mitt.listfriend().add(dest);
							dest.friendship(mitt, server);
							writer.write("Ora tu e" + " " + destinatario + " " + "siete amici !!");
							writer.newLine(); 
							writer.flush();
						}
					}
					
				}
				else if (jsonObject.containsKey("listfriend")){
					
					String utente = (String) jsonObject.get("listfriend");
					
					for (Utente u : utenti.values())
						if (u.getnome().equals(utente)){
							JSONObject array = new JSONObject();
							JSONArray amici = new JSONArray();
							
							for (Utente g : u.listfriend()){
								JSONObject amico= new JSONObject ();
								amico.put("utente", g.getnome());
								amico.put("stato", g.getstato());
								amici.add(amico);
								
								array.put("amici", amici);
							}
							
							writer.write(array.toJSONString());
							writer.newLine(); 
							writer.flush(); 
						}			
			   }
			else if (jsonObject.containsKey("chatroom")){
				JSONObject messaggio = (JSONObject) jsonObject.get("chatroom");
				String nome = (String) messaggio.get("nome");
				String creatore = (String) messaggio.get("creatore");
				String ip=null;
				Boolean flag=false;
				String s = null;
				int p=0;
				
				if (chatroom !=null && chatroom.containsKey(nome)){
					s=new String("Nome già in uso, non è possibile creare la ChatRoom");
					ip=new String("0.0.0.0");
				}	
				else
				{	Utente u=utenti.get(creatore);
					fine_ip++;
					ip=IP+((new Integer(fine_ip)).toString());
					ChatRoom c = new ChatRoom(nome,u,ip);
					u.addGorup(c);
					chatroom.put(nome,c);
					s=new String("ChatRoom " + nome + " è stata creata con successo");
				}			
				
				writer.write(s);
				writer.newLine(); 
				writer.flush(); 
				
				writer.write(ip);
				writer.newLine(); 
				writer.flush(); 
				
				
			}
			else if (jsonObject.containsKey("chatroomlist")){
				String nome = (String) jsonObject.get("chatroomlist");
				JSONObject array = new JSONObject();
				JSONArray rooms = new JSONArray();
				String s;
				
				for (ChatRoom c: chatroom.values()){
					System.out.println(c.getNome());
					JSONObject chat= new JSONObject ();
					chat.put("nome", c.getNome());
							
					if (c.partecipante(nome))
						s=new String("iscritto");
					else
						s=new String("");
							
					chat.put("partecipante", s);
					rooms.add(chat);
					
				}
				
				array.put("chatroomlist", rooms);
				
				writer.write(array.toJSONString());
				writer.newLine(); 
				writer.flush();		
			}
			else if (jsonObject.containsKey("iscritti chat")){
				JSONObject messaggio = (JSONObject) jsonObject.get("iscritti chat");
				String chat = (String) messaggio.get("chat");
				String utente = (String) messaggio.get("utente");
				
				if (chatroom.containsKey(chat)){
					ChatRoom c=chatroom.get(chat);
					writer.write("OK");
					writer.newLine(); 
					writer.flush();
					
					JSONObject array = new JSONObject();
					JSONArray iscritti = new JSONArray();
					
					String creatore;
					String iscritto;
					
					for (Utente u: c.partecipanti()){
						JSONObject ogg= new JSONObject ();
						ogg.put("nome", u.getnome());
						ogg.put("stato", u.getstato());
						iscritti.add(ogg);
					}
					
					array.put("list iscritti",iscritti);
					
					creatore=c.getCreatore().getnome();
					
					if (c.partecipante(utente))
						iscritto=new String("si");
					else
						iscritto=new String("no");
						
					array.put("creatore", creatore);
					array.put("iscritto", iscritto);
					
					writer.write(array.toJSONString());
					writer.newLine(); 
					writer.flush();
					
				}
				else
				{
					writer.write("La chatroom " + chat + " non esiste");
					writer.newLine(); 
					writer.flush();
				}
		}
		else if (jsonObject.containsKey("iscrizione chat")){
				JSONObject messaggio = (JSONObject) jsonObject.get("iscrizione chat");
				String chat = (String) messaggio.get("chat");
				String utente = (String) messaggio.get("utente");
				
				Utente u = utenti.get(utente);
				ChatRoom c= chatroom.get(chat);
				c.addpartecipante(u);
				u.addGorup(c);
				
				writer.write("Iscrizione alla chatroom " + chat + " avvenuta con successo");
				writer.newLine(); 
				writer.flush();
				
				writer.write(chatroom.get(chat).getIP());
				writer.newLine(); 
				writer.flush(); 
				
				
		}
		else if (jsonObject.containsKey("lascia chat")){
			JSONObject messaggio = (JSONObject) jsonObject.get("lascia chat");
			String chat = (String) messaggio.get("chat");
			String utente = (String) messaggio.get("utente");
			
			Boolean flag=false;
			
			Utente u = utenti.get(utente);
			ChatRoom c= chatroom.get(chat);
			c.removepartecipante(u);								
				
			writer.write("Hai lasciato la chatroom " + chat);
			writer.newLine(); 
			writer.flush();
			
			writer.write(c.getIP());
			writer.newLine(); 
			writer.flush();
					
		}
		else if (jsonObject.containsKey("chiudi chat")){
			JSONObject messaggio = (JSONObject) jsonObject.get("chiudi chat");
			String chat = (String) messaggio.get("chat");
			String utente = (String) messaggio.get("utente");
			
			Boolean flag=false;
			
			Utente u = utenti.get(utente);
			ChatRoom c= chatroom.get(chat);
											
			writer.write("Hai chiuso la chatroom " + chat);
			writer.newLine(); 
			writer.flush();
			
			writer.write(c.getIP());
			writer.newLine(); 
			writer.flush();
			
			for (Utente g : c.partecipanti())
				g.getGroups().remove(c);
			
			c.disabilita(server);
			chatroom.remove(chat);
			
		}
		else if (jsonObject.containsKey("invia file")){
			JSONObject messaggio = (JSONObject) jsonObject.get("invia file");
			String mittente = (String) messaggio.get("mittente");
			String destinatario = (String) messaggio.get("destinatario");
			String IP = null;
			int p = 0;
			
			Boolean flag=false;
			Utente u_mittente = null;
			
			
			u_mittente=utenti.get(mittente);
			
			for (Utente u : u_mittente.listfriend())
				if (u.getnome().equals(destinatario)){
					IP=u.getIP();
					p=u.getPorta_file();
					flag=true;
				}
			
			if (flag && IP!=null){
				writer.write(IP);
				writer.newLine(); 
				writer.flush();
				
				writer.write((new Integer(p)).toString()); 
				writer.newLine();
				writer.flush();
			}
			
			if (!flag)
			{
				writer.write("Utente non trovato");
				writer.newLine(); 
				writer.flush();
			}
			
			if (IP==null){
				writer.write("Utente offline, non è possibile inviare il file");
				writer.newLine(); 
				writer.flush();
			}
			
		}
		else if (jsonObject.containsKey("exit")){
			String utente = (String) jsonObject.get("exit");
			
			Utente u=utenti.get(utente);
			u.SetOffline();
		}
				
		socket.close();
							
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}