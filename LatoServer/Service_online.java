package LatoServer;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


/* Thread che ciclicamente, ogni 10 secondi, imposta connessioni TCP con i client per testare se sono online,
 * in maniera tale da tenere lo stato degli utenti aggiornato */

public class Service_online implements Runnable {

	/* hashmap concorrente che contiene gli utenti */
	private ConcurrentHashMap<String,Utente> utenti;

		public Service_online(ConcurrentHashMap<String,Utente> utenti){
		this.utenti=utenti;
	}
	
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		
		while (true){
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			for (Utente u: utenti.values()){
				if (u.getOnline()){
					try {
						Socket socket = new Socket(u.getIP(), u.getPorta_online());
						socket.close();
					} catch (IOException e) {
						/* se non è possibile impostare la connessione imposto l'utente offline */
						u.SetOffline();
					}
				}
			}
			
		}
				
	}

}
