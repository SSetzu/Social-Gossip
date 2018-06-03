package LatoClient;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

import LatoServer.ServerInterface;


public class GossipInterface extends JFrame implements ActionListener {

	/* HashMap concorrente utilizzata per riconoscere la finestra da visualizzare nel caso arrivi un messaggio
	 * da un utente o un messaggio di una chatroom, praticamente serve per capire se la finestra è già visibile 
	 * a schermo o deve essere istanziata quando arriva il messaggio */
	private ConcurrentHashMap<String,Interfaccia_Chat> user_frames = new ConcurrentHashMap<String,Interfaccia_Chat>();
	private ConcurrentHashMap<String,Interfaccia_ChatRoom> chatroom_frames = new ConcurrentHashMap<String,Interfaccia_ChatRoom>();
	
	/* componenti finestra principale */
	private JPanel panel1= new JPanel();
	
	private JLabel list = new JLabel("lista amici");
	private JLabel listroom = new JLabel("chat room");
	private JLabel statusMsg = new JLabel("Messaggi");
	private JTextArea printArea= new JTextArea();
	private JTextField bsearch = new JTextField();
		
	/* JList risultati di ricerca */
	private JList<String> res = new JList<String>();
	private DefaultListModel<String> listmodel;
	
	private JList<String> list_amici = new JList<String>();
	private DefaultListModel<String> listmodel_amici;
	
	private JList<String> list_chatroom = new JList<String>();
	private DefaultListModel<String> listmodel_chatroom;
	
	private JLabel search = new JLabel("cerca");
	private JButton amicizia= new JButton("chiedi amicizia");
	private JLabel lnot = new JLabel("Notifiche");
	private JTextArea not= new JTextArea();
	private JScrollPane scres= new JScrollPane(res);
	private JScrollPane scrollPaneStatus = new JScrollPane(list_amici);
	private JScrollPane scrollPane_not = new JScrollPane(not);
	private JScrollBar SB=new JScrollBar();
	private JScrollPane scrollPane_chatroom= new JScrollPane(list_chatroom);
	private JButton crea_room = new JButton("crea chat room");
		
	/* nome utente */
	private String utente;
	
	/* Ip server */
	private String IP;
	
	/* porta connessione di controllo */
	private int port;
	
	/* lista indicizzata che contiene gli indirizzi multicast dei
	 * gruppi a cui sono iscritto */
	private ArrayList<String> a;
	
	private ServerSocketChannel serverChannel_file;
	private ServerSocket socketmessaggi;
	private ServerSocket socketonline;
	
	/* Runnable che sta in ascolto sulla chatroom */
	private Ascolta_chatroom AC;
	
	
	public GossipInterface(String utente, int port, String IP, ArrayList<String> a, ServerSocketChannel serverChannel_file, ServerSocket socketmessaggi, ServerSocket socketonline){ 
		this.port=port;
		this.utente=utente;
		this.IP=IP;
		this.a=a;
		this.serverChannel_file=serverChannel_file;
		this.socketmessaggi=socketmessaggi;
		this.socketonline=socketonline;
		
		setTitle("SOCIAL GOSSIP");
		setSize(1000,500);
		panel1.setLayout(null);
		
		// posizionamento componenti
		scrollPaneStatus.setBounds(40,50,100,360);
		list.setBounds(60, 20, 120, 30);
		listroom.setBounds(210, 20, 120, 30);
		scrollPane_chatroom.setBounds(190, 50, 100, 360);
		crea_room.setBounds(400,380, 150, 30);
		search.setBounds(700, 50, 100, 20);
		bsearch.setBounds(750, 50, 170, 20);
		scres.setBounds(700, 80, 220, 280);
		amicizia.setBounds(700, 380, 150, 30);
		lnot.setBounds(470, 20, 80, 30);
		scrollPane_not.setBounds(400, 50, 220, 310);
		
		
		/* Timer per la richiesta della lista amici e lista chatroom aggiornate.
		 * Impostato ogni 10 secondi per non intasare la rete */
		Timer time_amici = new Timer(10000, this);
		time_amici.setActionCommand("lista amici");
		time_amici.start();
		
		Timer time_chatroom = new Timer(10000, this);
		time_chatroom.setActionCommand("chat room");
		time_chatroom.start();
		
		/* Listener che aggiorna la ricerca ogni variazione della stringa */
		bsearch.getDocument().addDocumentListener(new DocumentListener() {
		    public void insertUpdate(DocumentEvent e) {search();}
		    public void removeUpdate(DocumentEvent e) { search(); }
		    public void changedUpdate(DocumentEvent e) {search();
		    }
		});
		
		/* modalita di selezione singola sulla JList dei risultati della ricerca utente */
		res.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		/* registrazione del listener del mouse sulle JList degli amici e delle chatroom,
		 * serve per intercettare il doppio click sull'elemento selezionato
		 */
		list_chatroom.addMouseListener(new MouseSelector("chatroom"));
		list_amici.addMouseListener(new MouseSelector("amici"));
		
		/* Area notifiche e risultati ricerca utente non editabili */
		printArea.setEditable(false);
		not.setEditable(false);
		
		/* barra di ricerca editabile */
		bsearch.setEditable(true);
		
		scrollPane_not.setVerticalScrollBar(SB);
		
		/* registrazione listener sui bottoni */
		amicizia.setActionCommand("amicizia");
		amicizia.addActionListener(this);
		crea_room.setActionCommand("crea chat room");
		crea_room.addActionListener(this);
		
		/* aggiungo i componenti */
		panel1.add(scrollPaneStatus);
		panel1.add(list);
		panel1.add(listroom);
		panel1.add(scrollPane_chatroom);
		panel1.add(crea_room);
		panel1.add(bsearch);
		panel1.add(scres);
		panel1.add(search);
		panel1.add(amicizia);
		panel1.add(scrollPane_not);
		panel1.add(lnot);
		
		add(panel1);
		
		Registry registry;
		ServerInterface server;
		NotifyEventInterface stub;
		try {
			System.setProperty("Server", IP);
			registry = LocateRegistry.getRegistry(1099);
			String name = "Server";
			server = (ServerInterface) registry.lookup(name);
			
			NotifyEventInterface callbackObj = new NotifyEventImpl(not, utente, chatroom_frames);
			stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj,0);
			server.registerForCallback(stub);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		/* thread che sta in ascolto dei messaggi di appello del server per testare
 		 * se l'utente è online */
		SonoOnline SV = new SonoOnline(socketonline);
		Thread tsv = new Thread(SV);
		tsv.start(); 
		
		/* thread che sta in ascolto dei messaggi della chat privata */
		Ascolta_messaggi AM = new Ascolta_messaggi(user_frames,utente,IP,socketmessaggi);
		Thread t = new Thread(AM);
		t.start();  
		
		/* thread che riceve i file */
		Ricevi_file RF = new Ricevi_file(serverChannel_file,this);
		Thread trf = new Thread(RF);
		trf.start(); 
		
		/* thread che sta in ascolto dei messaggi della chatroom */
		AC = new Ascolta_chatroom(chatroom_frames, utente);
		Thread t1 = new Thread(AC);
		t1.start();
		
		/* unisco l'utente ai gruppi degli indirizzi multicast ricevuti */
		for (int i=0; i<a.size(); i++)
			AC.Unisci_algruppo(a.get(i));
		
		
		/*Listener chiusura della finestra, quando chiudo la finestra principale
		 * mando un messaggio al server sulla connessione di controllo per dirgli che mi sono disconesso */
		addWindowListener(new WindowAdapter() {
	         public void windowClosing(WindowEvent event) {
	        	
	        	Socket socket = null;
	     		BufferedWriter writer = null;
	        	 
	        	try {
					socket = new Socket(IP, port);
					writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				 
	        	JSONObject messaggio = new JSONObject ();
				messaggio.put("exit",utente);
				
				try {
					writer.write(messaggio.toJSONString());
					writer.newLine(); 
					writer.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				
				System.exit(0);  
	         }     
	      });
		
		}
	
	/* funzione di ricerca dell'utente, mando il messaggio al server sulla connessione
	 * di controllo e ricevo la lista degli utenti trovati */
	public void search(){
		
		Socket socket = null;
		BufferedWriter writer = null;
		BufferedReader reader = null;
		
		if (bsearch.getText().length()>0){
			
			try {
				socket = new Socket(IP, port);
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (IOException | NullPointerException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		
	
			JSONObject ric = new JSONObject ();
			ric.put("ricerca",bsearch.getText());
			String stringa = ric.toJSONString();
			
			try {
				writer.write(stringa);
				writer.newLine(); 
				writer.flush();
				
				
				String line = reader.readLine();
				int num_utenti=(new Integer(line)).intValue();
				
				
				listmodel = new DefaultListModel<>();
				
				for (int i=0; i<num_utenti; i++){
						line = reader.readLine();
						listmodel.addElement(line);
				}
				
				res.setModel(listmodel);
				
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}			
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		
		/* catturo la stringa relativa al bottone cliccato */
		String command = evt.getActionCommand();
		Socket socket = null;
		BufferedWriter writer = null;
		BufferedReader reader = null;
		
		try {
			socket = new Socket(IP, port);
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException | NullPointerException e2) {
			e2.printStackTrace();
		}
		
		if (command.equals("amicizia")){
			
			/* ottengo il valore selezionato dalla JList dei risultati 
			 * di ricerca e invio il messaggio JSON sulla connessione
			 * di controllo del server */
			String selected=res.getSelectedValue();
			
			if (selected!=null){
				JSONObject amicizia = new JSONObject ();
				JSONObject invio = new JSONObject ();
				
				invio.put("mittente", utente);
				invio.put("destinatario", selected );
				amicizia.put("amicizia", invio);
				
				String stringa = amicizia.toJSONString();
				
				try {
			
					writer.write(stringa);
					writer.newLine(); 
					writer.flush();
					
					// messaggio di risposta del server
					String line = reader.readLine();
					
					// visualizzo a schermo la risposta del server 
					// e chiudo il socket
					JOptionPane.showMessageDialog(res, line);
					socket.close();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		else if (command.equals("lista amici")){
			JSONObject mess = new JSONObject ();
			
			/* mando il messaggio di richiesta della lista amici al
			 * server sulla connessione di controllo e visualizzo
			 * sulla JList della lista amici il risultato, visualizzando
			 * opportunamente lo stato dell'utente
			 */
			
			mess.put("listfriend", utente);
			try {
				writer.write(mess.toJSONString());
				writer.newLine(); 
				writer.flush();
				
				String line = reader.readLine();
				
				JSONParser parser = new JSONParser();
				Object obj = parser.parse(line);
				JSONObject jsonObject = (JSONObject) obj;
				JSONArray amici=  (JSONArray) jsonObject.get("amici");
				
				int aux_selected=list_amici.getSelectedIndex();
				
				listmodel_amici = new DefaultListModel<>();
				list_amici.setModel(listmodel_amici);
				
				if (amici != null)
					for (int i=0; i<amici.size(); i++){
						JSONObject Jogg = (JSONObject) amici.get(i);
						String utente= (String) Jogg.get("utente");
						String stato = (String) Jogg.get("stato");
						if (stato.equals("online"))
							listmodel_amici.addElement("• " + utente);
						else
							listmodel_amici.addElement("ø " + utente);
					}
				
				list_amici.setSelectedIndex(aux_selected);
						
				socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (org.json.simple.parser.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		else if (command.equals("crea chat room")){
			// input dialog per la richiesta del nome della chatroom
			String response = JOptionPane.showInputDialog(null,"Inserisci il nome della chatroom","Nome chatroom",JOptionPane.QUESTION_MESSAGE);
			
			// null se si preme annulla, in quel caso non si fa nulla
			if (response!=null){
				// si eliminano gli spazi vuoti dal nome della chatroom
				response=response.trim();
				if (response.length()<3 || response.length()>15){
					JOptionPane.showMessageDialog(panel1,"Il nome della chatroom ha un numero di caratteri non consentito");
					}
				else
				{
					/* costruzione e invio del messaggio di richiesta della creazione di una chatroom */
					JSONObject messaggio = new JSONObject ();
					JSONObject invio = new JSONObject ();
						
					invio.put("nome", response);
					invio.put("creatore", utente);
						
					messaggio.put("chatroom", invio);
					
					
					String stringa = messaggio.toJSONString();
						
					try {
					
						writer.write(stringa);
						writer.newLine(); 
						writer.flush();
						
						/* visualizzo la risposta a schermo */
						String line = reader.readLine();
						JOptionPane.showMessageDialog(res, line);
						
						/* leggo l'indirizzo ip multicast del gruppo creato, se non è stato possibile creare 
						 * il gruppo, l'indirizzo è impostato a 0.0.0.0 */
						String group = reader.readLine();
						
						/* se l'indirizzo è consistente il thread AC viene messo in ascolto sull'indirizzo in
						 * ascolto che abbiamo ricevuto */
						if (group!=null && !group.equals("0.0.0.0"))
							AC.Unisci_algruppo(group);
							
						socket.close();
							
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		else if (command.equals("chat room")){
			
			/* Lista chatroom, analogo a lista utenti */
			JSONObject messaggio= new JSONObject();
			
			messaggio.put("chatroomlist", utente);
			
			try {
				writer.write(messaggio.toJSONString());
				writer.newLine(); 
				writer.flush();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String line = null;
			try {
				line = reader.readLine();
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			JSONParser parser = new JSONParser();
			
			Object obj = null;
			try {
				obj = parser.parse(line);
			} catch (org.json.simple.parser.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject jsonObject = (JSONObject) obj;
			JSONArray array=  (JSONArray) jsonObject.get("chatroomlist");
			
			int aux_selected=list_chatroom.getSelectedIndex();
			
			listmodel_chatroom = new DefaultListModel<>();
			list_chatroom.setModel(listmodel_chatroom);
			
			if (array != null)
				for (int i=0; i<array.size(); i++){
					JSONObject Jogg = (JSONObject) array.get(i);
					String nome= (String) Jogg.get("nome");
					String stato = (String) Jogg.get("partecipante");
					listmodel_chatroom.addElement(nome);
				}
			
			list_chatroom.setSelectedIndex(aux_selected);
			
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}	
}
	
	/* Listener Mouse */
	class MouseSelector extends MouseAdapter{
		
		/* stringa associata al componente grafico, inizializzato nel costruttore */
		String componente;
		
		public MouseSelector(String s){
			this.componente=s;
		}
		
		/* Intercetto l'evento del click del mouse */
		public void mouseClicked(MouseEvent evt){
		
			/* doppio click, differenti casi a seconda del componente che ha intercettato l'evento */
			if (evt.getClickCount()==2){
				
				if (this.componente.equals("chatroom")){
				
					/* valore selezionato nella JList della lista chatroom */
					String chat=list_chatroom.getSelectedValue();
					
					/* mando un messaggio al server sulla connessione di controllo
					 * per ottenere la lista degli iscritti alla chatroom,
					 * in modo da poter passare la lista all'interfaccia della chatroom
					 */
					Socket socket = null;
					BufferedWriter writer = null;
					BufferedReader reader = null;
					try {
						socket = new Socket(IP, port);
						writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
						reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					} catch (IOException | NullPointerException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				
					JSONObject messaggio = new JSONObject();
					JSONObject contenuto = new JSONObject();
					
					contenuto.put("chat", chat);
					contenuto.put("utente", utente);
					
					messaggio.put("iscritti chat", contenuto);
					
					try {
						writer.write(messaggio.toJSONString());
						writer.newLine(); 
						writer.flush();
						
						
						String risposta = reader.readLine();
						
						/* in caso di fallimento visualizzo a schermo il messaggio di errore */
						if (!risposta.equals("OK")){
							JOptionPane.showMessageDialog(list_chatroom, risposta);
						}
						else
						{
							risposta = reader.readLine();
							
							JSONParser parser = new JSONParser();
							Object obj = null;
							try {
								obj = parser.parse(risposta);
							} catch (org.json.simple.parser.ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							JSONObject jsonObject = (JSONObject) obj;
							JSONArray array=  (JSONArray) jsonObject.get("list iscritti");
							String creatore = (String) jsonObject.get("creatore");
							String iscritto = (String) jsonObject.get("iscritto");
							
							/* lista indicizzata di stringhe usata per memorizzare 
							 * la lista degli utenti iscritti alla chatroom selezionata */
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
							
							/* inserisco il nome della chatroom nella hashmap concorrente dei JFrame 
							 * visibili a schermo */
							if (!chatroom_frames.containsKey(chat))
								chatroom_frames.put(chat,new Interfaccia_ChatRoom(u,creatore,iscritto,IP, port,utente,chat,chatroom_frames, AC));
							
							socket.close();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}		
				}
				else if (this.componente.equals("amici")){
					
					/* prendo la sottostringa ottenuta eliminando i primi due caratteri, utilizzati per
					 * visualizzare lo stato */
					String ricevente=list_amici.getSelectedValue().substring(2);
					
					/* inserisco il nome utente nella hashmap concorrente dei JFrame 
					 * visibili a schermo */
					if (!user_frames.containsKey(ricevente))
						user_frames.put(ricevente,new Interfaccia_Chat(utente, ricevente, user_frames,IP));
					
				}
			}
		}
	}	
}
