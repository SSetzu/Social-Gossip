package LatoClient;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


/* Interfaccia della chatroom */
public class Interfaccia_ChatRoom extends JFrame implements ActionListener {
	
	/* HashMap concorrente utilizzata per riconoscere la finestra da visualizzare nel caso arrivi un messaggio
	 * nella chatroom, praticamente serve per capire se la finestra è già visibile 
	 * a schermo o deve essere istanziata quando arriva il messaggio */
	private ConcurrentHashMap<String,Interfaccia_ChatRoom> chatroom_frames;
	
	/* lista indicizzata che contiene la lista di utenti iscritti alla chatroom,
	 * utile all'avvio dell'interfaccia grafica */
	private ArrayList<String> utenti;
	
	/* timer utilizzato per aggiornare lo stato degli utenti della chatroom */
	private Timer time_chatroom;
	
	/* thread che ascolta i messaggi della chatroom */
	private Ascolta_chatroom AC;
	
	/* nome del creatore della chat */
	private String creatore;
	
	/* può essere "si" o "no", indica se l'utente è iscritto
	 * alla chatroom, stringa ricevuta dal server */
	private String iscritto;
	
	/* nome utente */
	private String utente;
	
	/* nome chatroom */
	private String chat;
	
	/* IP e porta del server, porta relativa alla connessione di controllo */
	private String IP;
	private int porta;

	/* componenti grafici */
	private JPanel panel=new JPanel();
	private JTextArea printArea= new JTextArea();
	private JScrollPane scrollPane = new JScrollPane(printArea);
	private JButton Iscriviti = new JButton("iscriviti");
	private JButton Chiudichat = new JButton("chiudi chat");
	private JButton Lasciachat = new JButton("lascia chat");
	private JTextArea lettura = new JTextArea();
	private JScrollPane scrollPane_lettura = new JScrollPane(lettura);
	private JTextField scrittura = new JTextField();
	private JButton invia = new JButton("invia");
	
	public Interfaccia_ChatRoom(ArrayList<String> u, String creatore, String iscritto, String IP, int porta, String utente,String nome_chat, ConcurrentHashMap<String,Interfaccia_ChatRoom> chatroom_frames, Ascolta_chatroom AC) {
		utenti=u;
		this.creatore=creatore;
		this.iscritto=iscritto;
		this.utente=utente;
		this.chat=nome_chat;
		this.porta=porta;
		this.IP=IP;
		this.chatroom_frames=chatroom_frames;
		this.AC=AC;
		
		setTitle("SOCIAL GOSSIP - " + chat);
		setSize(320, 320);
		setResizable(false);
		
		panel.setLayout(null);
		printArea.setEditable(false);
		
		/* posizionamento componenti e registrazione listener */
		scrollPane.setBounds(10, 10, 100, 180);
		scrollPane_lettura.setBounds(120,10,180,180);
		Iscriviti.setBounds(10,200,80, 30);
		Lasciachat.setBounds(90,200,100, 30);
		Chiudichat.setBounds(190,200,100, 30);
		scrittura.setBounds(10,240,180,30);
		invia.setBounds(190,240,100, 30);
		
		scrittura.addKeyListener(new MyKeyListener());
		Iscriviti.setActionCommand("iscrizione");
		Iscriviti.addActionListener(this);
		Lasciachat.setActionCommand("lascia chat");
		Lasciachat.addActionListener(this);
		Chiudichat.setActionCommand("chiudi chat");
		Chiudichat.addActionListener(this);
		invia.setActionCommand("invia chatroom");
		invia.addActionListener(this);
		
		
		/* timer lista iscritti, impostato a 10 secondi */
		time_chatroom = new Timer(10000, this);
		time_chatroom.setActionCommand("lista iscritti");
		time_chatroom.start();
		
		/* aggiungo i componenti al pannello */
		panel.add(scrollPane);
		panel.add(Iscriviti);
		panel.add(Lasciachat);
		panel.add(Chiudichat);
		panel.add(scrollPane_lettura);
		panel.add(scrittura);
		panel.add(invia);
		
		/* appendo la lista di utenti iscritti alla chatroom nella textbox */
		for (String s : u)
			printArea.append(s + "\n");
			
		add(panel);
		
		
		/* a seconda che l'utente sia iscritto o no, o che sia il creatore
		 * della chatroom abilito e diabilito bottoni */
		
		if (iscritto.equals("no")){
			Lasciachat.setEnabled(false);
			Iscriviti.setEnabled(true);
			invia.setEnabled(false);
		}
		else
		{
			Lasciachat.setEnabled(true);
			Iscriviti.setEnabled(false);
			invia.setEnabled(true);
		}
		
		if (creatore.equals(utente))
			Lasciachat.setEnabled(false);
		else
			Chiudichat.setEnabled(false);
	
	// listener finestra
	addWindowListener(new WindowAdapter(){
		
		// chiusura della finestra
		public void windowClosing(WindowEvent e){
			exit();
		}
		
	
	});
		
	setVisible(true);
	}

	
	/* fermo il timer della lista iscritti, rimuovo il JFrame relativo alla chatroom dalla
	 * hashmap delle finestre visibili, imposto la visibilita del JFrame a false e 
	 * libero le risore grafiche
	 */
	public void exit(){
		time_chatroom.stop();
		chatroom_frames.remove(chat);
		setVisible(false);
		dispose();
	}
	
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		
		BufferedWriter writer = null;
		BufferedReader reader = null;
		Socket socket = null;
		
		// socket sulla connessione di controllo
		try{	
			socket = new Socket(IP, porta);
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException | NullPointerException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	
		/* catturo la stringa relativa al bottone cliccato */
		String command = evt.getActionCommand();
		
		if (command.equals("iscrizione")){
			
			JSONObject messaggio = new JSONObject ();
			JSONObject invio = new JSONObject ();
			
			messaggio.put("utente", utente);
			messaggio.put("chat",chat);
			invio.put("iscrizione chat", messaggio);
			
			
			try {
				writer.write(invio.toJSONString());
				writer.newLine();
				writer.flush();
				
				String line = reader.readLine();
				JOptionPane.showMessageDialog(panel, line);
				
				/* ricevo l'ip multicast relativo al gruppo 
				 * a cui mi sono iscritto e mi unisco al gruppo*/
				String group = reader.readLine();
				 
				AC.Unisci_algruppo(group);
				
				exit();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		else if (command.equals("lascia chat")){
			
			JSONObject messaggio = new JSONObject ();
			JSONObject invio = new JSONObject ();
			
			messaggio.put("utente", utente);
			messaggio.put("chat",chat);
			invio.put("lascia chat", messaggio);
			
			
			try {
				writer.write(invio.toJSONString());
				writer.newLine();
				writer.flush();
				
				String line = reader.readLine();
				JOptionPane.showMessageDialog(panel, line);
				
				/* ricevo l'ip multicast relativo al gruppo 
				 * che ho lasciato e lascio il gruppo*/
				String group= reader.readLine();
				
				AC.lascia_gruppo(group);
				
				exit();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		else if (command.equals("chiudi chat")){
			
			JSONObject messaggio = new JSONObject ();
			JSONObject invio = new JSONObject ();
			
			messaggio.put("utente", utente);
			messaggio.put("chat",chat);
			invio.put("chiudi chat", messaggio);
			
			try {
				writer.write(invio.toJSONString());
				writer.newLine();
				writer.flush();
				String line = reader.readLine();
				JOptionPane.showMessageDialog(panel, line);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
			exit();
			
		}
		else if (command.equals("lista iscritti")){
			
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
				
				if (!risposta.equals("OK")){
					JOptionPane.showMessageDialog(panel, risposta);
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
			
			
					printArea.setText("");
					
					if (array != null)
						for (int i=0; i<array.size(); i++){
							JSONObject Jogg = (JSONObject) array.get(i);
							String nome= (String) Jogg.get("nome");
							String stato = (String) Jogg.get("stato");
							
							if (stato.equals("online"))
								printArea.append("• " + nome + "\n");
							else
								printArea.append("ø " + nome + "\n");
						}
				}
			} catch (IOException e) {
					e.printStackTrace();
			}	
		}
		else if (command.equals("invia chatroom")&&(scrittura.getText().length()>0)){
			/* invio un pacchetto UDP al server sulla porta dei messaggi della chatroom */
			
			
			JSONObject messaggio = new JSONObject();
			JSONObject body = new JSONObject();
			// porta messaggi chatroom
			int porta=5002;
			// indirizzo server
			String Name="localhost";
			InetAddress address=null;
			
			byte[] buffer = new byte[2048];
			
			body.put("mittente", utente);
			body.put("destinatario", chat);
			body.put("body", "<"+utente+">" + ":"+ scrittura.getText());
			messaggio.put("messaggio chatroom", body);
			String s=messaggio.toJSONString();
			
			
			try {
				address=InetAddress.getByName(Name);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				System.out.printf("ERR %s 1\n",Name);
				return;
			}
			
			DatagramSocket sock=null;
			
			try {
				sock = new DatagramSocket();
			} catch (SocketException e) {
				System.out.printf("ERR %d 2\n",porta);
				return;
			}
			
			buffer= s.getBytes();
			DatagramPacket mypacket = new DatagramPacket(buffer,buffer.length,address,porta);
			
			try {
				sock.send(mypacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sock.close();
		}
		scrittura.setText("");
	}
	
	public JTextArea getTextArea(){
		return lettura;
	}
	
	/* Listener della tastiera */
	class MyKeyListener extends KeyAdapter{
		
		
		/* catturo il tasto invio e attivo la funzione
		 * doClick, che simula la pressione del bottone
		 * da cui viene chiamata, praticamente, quando
		 * premo invio e come se premo invia
		 */
		public void keyPressed(KeyEvent evt){
			if (evt.getKeyChar()==KeyEvent.VK_ENTER){
				invia.doClick();
				}
		}
		
		public void keyTyped(KeyEvent evt){
			if (evt.getKeyChar()==KeyEvent.VK_ENTER){
				scrittura.setText("");
				scrittura.grabFocus();
			}
		}
	}
}