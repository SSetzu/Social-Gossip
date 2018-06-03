package LatoClient;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;



public class CreateListener extends JFrame implements ActionListener {

	/* componenti finestra di registrazione */
	private JPanel panel1= new JPanel();
	private JLabel L_Utente1 = new JLabel("Nome Utente");
	private JTextField T_Utente1 = new JTextField(20);
	private JLabel L_password1 = new JLabel("Password");
	private JPasswordField T_password1 = new JPasswordField(20);
	private JLabel L_password2 = new JLabel("Conferma Password");
	private JPasswordField T_password2 = new JPasswordField(20);
	private JButton Registrati1 = new JButton("Registrati");
	private JLabel lab_lingua =new JLabel("Lingua");
	private JComboBox lingua;
	
	private ArrayList<String> tags = new ArrayList<String>();
	private ArrayList<String> lingue = new ArrayList<String>();
	
	private JFrame login;
	
	public CreateListener(JFrame login){
		
		this.login=login;
		
		
		try {
			/* Utilizzo della libreria esterna JSOUP per la scelta del parametro lingua.  
			 * Gaccio una richiesta http GET all'URL indicato seleziono il contenuto dei tag <tr[valign=top]> 
			 * e a sua volta seleziono il contenuto dei tag td, prendo le righe a 5 a 5 e estraggo la stringhe 
			 * che mi servono.
			 */
			Document doc = Jsoup.connect("http://www.loc.gov/standards/iso639-2/php/code_list.php").get();
			Elements table = doc.select("tr[valign=top]");
			Elements rows = table.select("td");
			
			for (int i=0; i<rows.size(); i=i+5){
				
				String tag = rows.get(i+1).text();
				String name = rows.get(i+2).text();
				
				if (!tag.equals("")){
					tags.add(tag);
					lingue.add(name);
				}
					
			}
			/* se non è possibile fare la richiesta http, il parametro lingua può
			 * essere scelto tra italiano e inglese */
			} catch (IOException e) {
			// TODO Auto-generated catch block
				tags.add("eng");
				tags.add("it");
				lingue.add("English");
				lingue.add("Italian");
			}
		
		/* tolgo la visibilità dell'interfaccia di login e rilascio le risorse grafiche */
		login.setVisible(false);
		login.dispose();
		
		/* interfaccia di registrazione */
		setTitle("SOCIAL GOSSIP - Registrazione");
		setSize(250,300); 
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		/* il contenuto del frame è il pannello panel1 */
		setContentPane(panel1);
		setResizable(false);
		
		panel1.setLayout(null);
		
		// posizionamento componenti 
		L_Utente1.setBounds(70,1,200,20);
		T_Utente1.setBounds(10,30,200,20);
		L_password1.setBounds(80,60,200,20);
		T_password1.setBounds(10,90,200,20);
		L_password2.setBounds(50,120,200,20);
		T_password2.setBounds(10,150,200,20);
		Registrati1.setBounds(50, 220, 100, 25);
		lab_lingua.setBounds(30,180,200,20);
		
		/* istanzio una ComboBox con l'array che ho riempito p
		 * prima */
		lingua=new JComboBox(lingue.toArray());
		lingua.setBounds(75,180,100,25);
		
		/* registrazione del listener della tastiera sulle textbox
		 * di utente e password e sulla combobox della lingua */
		T_Utente1.addKeyListener(new MyKeyListener());
		T_password1.addKeyListener(new MyKeyListener());
		T_password2.addKeyListener(new MyKeyListener());
		lingua.addKeyListener(new MyKeyListener());
		
		/* registrazione del listener sul tasto di registrazione */
		Registrati1.addActionListener(this);
		
		/* aggiungo tutte le componenti al pannello */
		panel1.add(lab_lingua);
		panel1.add(lingua);
		panel1.add(L_Utente1);
		panel1.add(T_Utente1);
		panel1.add(L_password1);
		panel1.add(T_password1);
		panel1.add(L_password2);
		panel1.add(T_password2);
		panel1.add(Registrati1);
		
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
		/* estrazione stringhe dai componenti */
		String t= T_Utente1.getText();
		String p=new String(T_password1.getPassword());
		String p2=new String(T_password2.getPassword());
		String tag = tags.get(lingua.getSelectedIndex());
		
		if (!p.equals(p2)){
			JOptionPane.showMessageDialog(panel1, "Le 2 password non coincidono");
			return;
		}
		
		
		if (t.length()<3 || t.length()>15){
			JOptionPane.showMessageDialog(panel1, "Il nome utente ha un numero di caratteri non consentito");
			return;
		}
		
		if (p.length()<3 || p.length()>15){
			JOptionPane.showMessageDialog(panel1, "La password ha un numero di caratteri non consentito");
			return;
		}
			
		JSONObject reg = new JSONObject ();
		JSONObject info = new JSONObject ();
		
		info.put("utente", T_Utente1.getText());
		info.put("password", p);
		info.put("lingua", tag);
		
		
		reg.put("registrazione", info);
				
		String stringa = reg.toJSONString();
			
		/* indirizzo e porta di controllo del server */
		String hostname = "localhost"; int port = 5000; 
		
		try { 
			Socket socket = new Socket(hostname, port);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			
			/* mando al server il messaggio JSON di registrazione */
			writer.write(stringa);
			writer.newLine(); 
			writer.flush();
			
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			/* risposta del server */
			String line = reader.readLine();
			
			if (line.equals("Registrazione Negata")){
				JOptionPane.showMessageDialog(panel1, "Regitrazione negata, nome utente già in uso");
				
			}else if (line.equals("Registrazione Consentita")){
				JOptionPane.showMessageDialog(panel1, "Registrazione avvenuta con successo");
				
				/* tolgo la visibilità dell'interfaccia di registrazione e rilascio le risorse grafiche */
				this.setVisible(false);
				this.dispose();
				
				/* istanzio una nuova interfaccia di login e la visualizzo a schermo */
				GUIclient gui = new GUIclient();
				gui.setVisible(true);
			}
			
			socket.close();
			
			} catch (IOException e){
			e.printStackTrace();
		}		
	}
	
	/* Listener della tastiera */
	class MyKeyListener extends KeyAdapter{
		
		/* catturo il tasto invio e attivo la funzione
		 * doClick, che simula la pressione del bottone
		 * da cui viene chiamata, praticamente quando
		 * premo invio e come se premo registrati
		 */
		public void keyPressed(KeyEvent evt){
			if (evt.getKeyChar()==KeyEvent.VK_ENTER){
				Registrati1.doClick();
				}
		}
	}
}