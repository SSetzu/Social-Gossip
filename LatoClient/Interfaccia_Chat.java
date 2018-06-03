package LatoClient;
import javax.swing.*;
import org.json.simple.JSONObject;

import java.awt.event.*;		
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/* Interfaccia della chat privata */
class Interfaccia_Chat extends JFrame implements ActionListener{

	// componenti grafici 
	private JTextArea lettura = new JTextArea();
	private JTextArea scrittura = new JTextArea();
	private JButton esci = new JButton("invia file");
	private JButton invia = new JButton("invia");
	private JPanel pan=new JPanel();
	private JScrollPane scrollettura;
	private JScrollPane scrolscrittura;
	private JScrollBar SB=new JScrollBar();
	private JLabel L1 = new JLabel();
	private JLabel L2 = new JLabel();
	
	// nome del mittente
	private String nick;
	
	// nome del destinatario
	private String ricevente;
	
	// IP server
	private String IP;
	
	/* HashMap concorrente utilizzata per riconoscere la finestra da visualizzare nel caso arrivi un messaggio
	 * da un utente, praticamente serve per capire se la finestra è già visibile 
	 * a schermo o deve essere istanziata quando arriva il messaggio */
	private ConcurrentHashMap<String,Interfaccia_Chat> user_frames;

	public Interfaccia_Chat(String nick, String ricevente, ConcurrentHashMap<String,Interfaccia_Chat> user_frames, String IP){

		this.user_frames=user_frames;
		this.nick=nick;
		this.ricevente=ricevente;
		this.IP=IP;

		setTitle("SOCIAL GOSSIP - " + ricevente);
		setSize(320, 250);
		setContentPane(pan);
		pan.setLayout(null);
		setResizable(false);
		
		/* posizionamento componenti e registrazione listener */
		lettura.setBounds(10,40,300,85);
		lettura.setEditable(false);
		lettura.setLineWrap(true);
		scrollettura = new JScrollPane(lettura);
		scrollettura.setBounds(10,40,300,85);
		scrollettura.setVerticalScrollBar(SB);
	
		scrittura.addKeyListener(new MyKeyListener());
		scrittura.setBounds(10,175,300,20);
		scrittura.setLineWrap(true);
		scrolscrittura = new JScrollPane(scrittura);
		scrolscrittura.setBounds(10,175,300,20);
	
		esci.setBounds(100,140,100,25);
		esci.setActionCommand("INVIA FILE");
		esci.addActionListener(this);
	
		invia.setBounds(210,140,100,25);
		invia.setActionCommand("INVIA");
		invia.addActionListener(this);

		L1.setBounds(10,10,150,25);
		L1.setText("...con "+ricevente);
	
		L2.setBounds(10,140,100,25);
		L2.setText(nick+" :");
	
		pan.add(scrolscrittura);
		pan.add(scrollettura);
		pan.add(esci);
		pan.add(invia);
		pan.add(L1);
		pan.add(L2);
	
		setVisible(true);
		
		/* Listener finestra */ 
		addWindowListener(new WindowAdapter(){
			
			/* intercetto l'evento di chiusura della finestra, rimuovo 
			 * il frame dalla hashmap concorrente dei frame visibili
			 * imposto la visibilità della finestra a false
			 * e rilascio le risorse grafiche  */
			public void windowClosing(WindowEvent e){
				user_frames.remove(ricevente);
				setVisible(false);
				dispose();
			}
		}
	);
}

	public void actionPerformed(ActionEvent evt){
	
		/* catturo la stringa relativa al bottone cliccato */
		String command = evt.getActionCommand();
		
		if (command.equals("INVIA FILE")){
			
			Socket socket = null;
			BufferedWriter writer = null;
			BufferedReader reader = null;
			
			try {
				// socket connessione di controllo
				socket = new Socket(IP, 5000);
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (IOException | NullPointerException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			
			String path=null;
			/* istanzio un selettore di file e un menu grafico per la selezione del file, 
			 * facendo partire la ricerca sul file system dalla cartella home */
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
			int result = fileChooser.showOpenDialog(new JDialog());
			
			
			
			if (result == JFileChooser.APPROVE_OPTION) {
			    File selectedFile = fileChooser.getSelectedFile();
			    path =selectedFile.getAbsolutePath();
			}
			
			/* invio sulla connessione di controllo il messaggio di invio file */
			if (path!=null){
				JSONObject messaggio= new JSONObject();
				JSONObject corpo = new JSONObject();
				
				corpo.put("mittente", nick);
				corpo.put("destinatario", ricevente);
				messaggio.put("invia file",corpo);
				
				try {
					writer.write(messaggio.toJSONString());
					writer.newLine(); 
					writer.flush();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				/* In caso di successo ricevo come risposta dal server l'indirizzo IP e la porta in cui il client 
				 * riceve i file, in caso di fallimento il server invia un messaggio di errore e non invia la porta */
				String IP_ = null;
				int porta = 0;
				try {
					IP_ = reader.readLine();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				if (!IP_.equals("Utente non trovato") && !IP_.equals("Utente offline, non è possibile inviare il file")){
					try {
						porta=(new Integer(reader.readLine())).intValue();
					} catch (NumberFormatException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					/* thread che si occupa dell'invio del file */
					InviaFile inviafile = new InviaFile(IP_,porta,path,this);
					Thread t= new Thread(inviafile);
					t.start();
				
				
				try {
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				/* mando al server sulla connessione dei messaggi ( porta 5001) il messaggio di invio del file */
				String body ="<"+nick+"> :"+ "Ti ha inviato un file";
				JSONObject obj = new JSONObject();
				JSONObject busta = new JSONObject();
				busta.put("mittente", nick);
				busta.put("destinatario", ricevente);
				busta.put("corpo", body);
				obj.put("messaggio", busta);
				
				
				try {
					Socket Ugola=new Socket (IP,5001);
					Ugola.setSoTimeout(100000);
					BufferedWriter writer_ = new BufferedWriter(new OutputStreamWriter(Ugola.getOutputStream()));
					writer_.write(obj.toJSONString());
					writer_.newLine(); 
					writer_.flush();
				
				BufferedReader reader_ = new BufferedReader(new InputStreamReader(Ugola.getInputStream()));
					String s=reader_.readLine();
					
					if (!s.equals("OK")){
						JOptionPane.showMessageDialog(this, s);
					}
					else
						lettura.append(body + "\n");
					
					Ugola.close();
					} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					}
				}
				else
					JOptionPane.showMessageDialog(this, IP_);
			}
		}
			
		
		/* invio del messaggio */
		if (command.equals("INVIA")&&scrittura.getText().length()>0){
			String body =scrittura.getText();
			
		
			JSONObject obj = new JSONObject();
			JSONObject busta = new JSONObject();
			busta.put("mittente", nick);
			busta.put("destinatario", ricevente);
			busta.put("corpo", body);
			obj.put("messaggio", busta);
			
			
			try {
				Socket Ugola=new Socket (IP,5001);
				Ugola.setSoTimeout(100000);
				BufferedWriter writer_ = new BufferedWriter(new OutputStreamWriter(Ugola.getOutputStream()));
				writer_.write(obj.toJSONString());
				writer_.newLine(); 
				writer_.flush();
				
				BufferedReader reader_ = new BufferedReader(new InputStreamReader(Ugola.getInputStream()));
				String s=reader_.readLine();
				
				System.out.println(body);
				
				if (!s.equals("OK"))
					JOptionPane.showMessageDialog(this, s);
				else
					lettura.append("<"+nick+">: "+ body + "\n");
				
				Ugola.close();
				} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				}
			scrittura.setText("");
			scrittura.grabFocus();
		}
	}
	
	public JTextArea getTextField(){
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
