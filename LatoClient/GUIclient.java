package LatoClient;
import javax.swing.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.omg.CORBA.portable.InputStream;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;

public class GUIclient extends JFrame implements ActionListener  {
	
	
	/* componenti finestra di login */
	private JPanel panel= new JPanel();
	private JLabel L_Utente = new JLabel("Utente");
	private JTextField T_Utente = new JTextField(20);
	private JLabel L_password = new JLabel("Password");
	private JPasswordField T_password = new JPasswordField(20);
	private JButton Accedi = new JButton("Accedi");
	private JButton Registrati = new JButton("Registrati");
	
	
	public GUIclient(){
		
		setTitle("SOCIAL GOSSIP - Login");
		setSize(240,240);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setContentPane(panel);
		panel.setLayout(null);
		
		// posizionamento componenti 
		L_Utente.setBounds(80,1,200,20);
		T_Utente.setBounds(10,30,200,20);
		L_password.setBounds(80,60,200,20);
		T_password.setBounds(10,90,200,20);
		Accedi.setBounds(10,120,100,25);
		Registrati.setBounds(110, 120, 100, 25);
		
		/* registrazione del listener della tastiera sulle textbox
		 * di utente e password */
		T_Utente.addKeyListener(new MyKeyListener());
		T_password.addKeyListener(new MyKeyListener());
		
		/* registrazione dei listener sui bottoni
		 * e associazione delle stringhe ai comandi */
		Accedi.setActionCommand("Accedi");
		Accedi.addActionListener(this);
		 
		Registrati.setActionCommand("Registrati");
		Registrati.addActionListener(this);
		
		/* si aggiungono i componenti al pannello */
		panel.add(L_Utente);
		panel.add(T_Utente);
		panel.add(L_password);
		panel.add(T_password);
		panel.add(Accedi);
		panel.add(Registrati);
	}

	public static void main (String args[]){
		/* istanzio l'interfaccia e la rendo visibile
		 * a schermo */
		GUIclient gui = new GUIclient();
		gui.setVisible(true);
	}


	@Override
	public void actionPerformed(ActionEvent evt) {
		/* catturo la stringa relativa al bottone cliccato */
		String command = evt.getActionCommand();
		/* indirizzo del server */
		String hostname="127.0.0.1";
		/* porta in cui il server accetta le connsessioni
		 * di controllo */
		int port = 5000;
		
		if (command.equals("Accedi")){
			
			try { 
				Socket socket = new Socket(hostname, port);
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
				JSONObject login = new JSONObject ();
				JSONObject info = new JSONObject ();
				
				int porta_messaggi=0;
				int porta_file=0;
				int porta_online=0;
				
				/* socket che sta in ascolto dei messaggi della chat */
				ServerSocket socketmessaggi = null;
				
				/* provo tutte le porte non note fino a trovare una porta libera */
				for (int i = 1025; i<65536 ; i++){
					try {
						socketmessaggi = new ServerSocket(i);
						porta_messaggi=i;
						break;
					} catch (IOException  e1) {
						System.out.println("Non esiste un servizio sulla porta"+i);
					}	 	
				}	
				
				/* socket che sta in ascolto sulle connessioni del server per testare
				 * se l'utente è online */
				ServerSocket socketonline = null;
				
				/* provo tutte le porte non note fino a trovare una porta libera */
				for (int i = 1025; i<65536 ; i++){
					try {
						socketonline = new ServerSocket(i);
						porta_online=i;
						break;
					} catch (IOException  e1) {
						System.out.println("Non esiste un servizio sulla porta"+i);
					}	 	
				}
				
				/* socket per la ricezione del file */
				ServerSocketChannel serverChannel_file = null;
				 
				try {
					serverChannel_file = ServerSocketChannel.open();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				InetSocketAddress address=null;
				
				/* provo tutte le porte non note fino a trovare una porta libera */
				for (int i=1025;i<65536;i++){
					try {
						address = new InetSocketAddress(i);
						serverChannel_file.bind(address);
						porta_file=i;
						break;
					} catch (Exception e1) {
						System.out.println("Non esiste un servizio sulla porta"+i);
					}
				}	
				
				try {
					serverChannel_file.configureBlocking(true);
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				/* codice per test in rete locale, da ignorare
				 
				Document doc = Jsoup.connect("http://whatismyip.org").get();
				Elements table = doc.select("span");
		        String indirizzo = table.text();  */
		        
		        //String indirizzo=InetAddress.getLocalHost().getHostAddress();
				
				
				
				/* indirizzo del client */
				String indirizzo=InetAddress.getLoopbackAddress().getHostAddress();
		        
				
				/* costruzione messaggio di login */
				info.put("utente", T_Utente.getText());
				info.put("password", new String(T_password.getPassword()));
				info.put("indirizzo", indirizzo);
				info.put("porta messaggi", (new Integer(porta_messaggi)).toString());
				info.put("porta file", (new Integer(porta_file)).toString());
				info.put("porta online", (new Integer(porta_online)).toString());
				login.put("login", info);
				
				/* invio messaggio di login al server */
				writer.write(login.toJSONString());
				writer.newLine(); 
				writer.flush();
				
				/* ricevo la risposta dal server */
				String line = reader.readLine();
				
				/* accesso negato, chiudo tutti i socket e visualizzo a schermo un messaggio di
				 * fallimento */
				if (line.equals("Accesso Negato")){
					JOptionPane.showMessageDialog(this, line + " nome utente o password non validi");
					serverChannel_file.close();
					socketmessaggi.close();
					socketonline.close();
				}
				else if (line.equals("Accesso Consentito")){
					
					/* ricevo dal server la lista delle chatroom (nome chatroom, IP chatroom) a cui sono iscritto, 
					 * questo mi serve perchè devo unirmi ai gruppi multicast in modo da poter stare in ascolto 
					 * dei messaggi della chatroom */
					String s= reader.readLine();
					
					JSONParser parser = new JSONParser();
					Object obj = parser.parse(s);
					JSONObject jsonObject = (JSONObject) obj;
					JSONArray tue_chatroom=  (JSONArray) jsonObject.get("tue chatroom");
					
					/* ArrayList che contiene gli indirizzi IP dei gruppi, me lo salvo qua per poi
					 * passarlo a GossipInterface */
					ArrayList<String> a = new ArrayList<String>();
				
					if (tue_chatroom != null)
						for (int i=0; i<tue_chatroom.size(); i++){
							JSONObject Jogg = (JSONObject) tue_chatroom.get(i);
							String IP_room= (String) Jogg.get("IP chat");
							a.add(IP_room);
						}
					
					/* tolgo la visibilità dell'interfaccia di login, istanzio l'interfaccia
					 * principale e la visualizzo a schermo */
					this.setVisible(false);
					GossipInterface gi = new GossipInterface(T_Utente.getText(), port, hostname,a,serverChannel_file, socketmessaggi, socketonline);
					gi.setVisible(true);
				}
				else
					JOptionPane.showMessageDialog(this, "Accesso negato, nome utente o password non validi");
				
			} catch (IOException e){
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
		else if (command.equals("Registrati")){
			/* istanzio il listener e lo registro sul bottone registrati */
			ActionListener createListener= new CreateListener(this);
			Registrati.addActionListener(createListener);
		}	
	}
	
	/* Listener della tastiera */
	class MyKeyListener extends KeyAdapter{
		
		/* catturo il tasto invio e attivo la funzione
		 * doClick, che simula la pressione del bottone
		 * da cui viene chiamata, praticamente, quando
		 * premo invio e come se premo accedi
		 */
		public void keyPressed(KeyEvent evt){
			if (evt.getKeyChar()==KeyEvent.VK_ENTER){
				Accedi.doClick();
				}
		}
	}
}