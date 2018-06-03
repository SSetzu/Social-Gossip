package LatoServer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/* Thread che si occupa di ricevere il messaggio sul socket passato e di inoltrarlo 
 * all'utente destinatario, utilizzando la coppia <IP, porta_messaggi> relativi
 * all'utente destinatario */
public class Task_message implements Runnable {

	private Socket socket;
	ConcurrentHashMap<String,Utente> utenti;
	
	public Task_message(Socket socket, ConcurrentHashMap<String,Utente> utenti){
		this.socket=socket;
		this.utenti=utenti;
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	try{
		
		BufferedReader reader;
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String s = reader.readLine();
		
		JSONParser parser = new JSONParser();
		Object obj=null;
		try {
			obj = parser.parse(s);
		} catch (ParseException | NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONObject jsonObject = (JSONObject) obj;
		
		System.out.println(jsonObject);
		
		JSONObject richiesta = (JSONObject) jsonObject.get("messaggio");
		String mittente = (String) richiesta.get("mittente");
		String destinatario = (String) richiesta.get("destinatario");
		String corpo = (String) richiesta.get("corpo");
		
		String lingua_mittente = null;
		String lingua_destinatario = null;
		Boolean flag=false;
		Utente u_mittente = null;
		
		String str = null;
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		
		
		Utente g=utenti.get(mittente);
		lingua_mittente=g.getLingua();
		u_mittente=g;
		
		for (Utente u: u_mittente.listfriend())
			if (u.getnome().equals(destinatario)){
				flag=true;
				if (u.getIP()==null)
					 str = new String("L'utente " + destinatario + " non è online, pertanto non visualizzerà il tuo messaggio" );
				else
				{
						lingua_destinatario=u.getLingua();
					
						if (!lingua_mittente.equals(lingua_destinatario)){
						
							String request =new String("https://api.mymemory.translated.net/get?q="+corpo+"&langpair="+lingua_mittente+"|"+lingua_destinatario);
							// sostituisce gli spazi con %20
							ParseURL a = new ParseURL(request);
							request=a.Parse();
							System.out.println(request);
							URL urlImage=new URL(request);
							HttpURLConnection uc=(HttpURLConnection) urlImage.openConnection();
							uc.connect();
							
							BufferedReader in=new BufferedReader(new InputStreamReader(uc.getInputStream(),Charset.forName("UTF-8")));
							
							String line=null;
							StringBuffer sb=new StringBuffer();
							while((line=in.readLine())!=null)
								sb.append(line);
							 
							
							JSONObject ogg = null;
							try {
								 ogg = (JSONObject) new JSONParser().parse(sb.toString());
							} catch (ParseException e) { 
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							JSONObject jsontradotto = (JSONObject) ogg.get("responseData");
							corpo = (String) jsontradotto.get("translatedText");	
								 
						}
						
						Socket Ugola = null;
					
						try{
						Ugola =new Socket (u.getIP(),u.getPorta_messaggi());
						} catch (ConnectException | NullPointerException e){
							
							writer.write("Non puoi inviare un messaggio a "+ u.getnome() + ", l'utente è offline");
							writer.newLine(); 
							writer.flush();
							
							socket.close();
							return;
						}
						
						Ugola.setSoTimeout(100000);
				
						BufferedWriter writer_ = new BufferedWriter(new OutputStreamWriter(Ugola.getOutputStream()));
				
						writer_.write(mittente);
						writer_.newLine(); 
						writer_.flush();
						
						writer_.write(corpo);
						writer_.newLine(); 
						writer_.flush(); 
						Ugola.close();
						
						str = new String("OK");
					}
					
				}
			
				
				if (!flag)
					str=new String("L'utente "+ destinatario +" non è nella tua lista amici, non è possibile inviare il messaggio");
				
				writer.write(str);
				writer.newLine(); 
				writer.flush();
				
				socket.close();
			
		} catch (IOException | NullPointerException e1 ) { 
			e1.printStackTrace();
		}
	}
	
}