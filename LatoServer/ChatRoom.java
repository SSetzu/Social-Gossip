package LatoServer;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class ChatRoom {
	
	// nome della chatroom
	private String nome;
	// nome creatore della chatroom
	private Utente creatore;
	// lista indicizzata utenti della partecipanti
	private ArrayList<Utente> partecipanti;
	// IP multicast della chatroom
	private String IP;
	
	
	public ChatRoom( String nome, Utente creatore, String IP){
		this.nome=nome;
		this.creatore=creatore;
		this.IP=IP;
		partecipanti=new ArrayList<Utente>();
		partecipanti.add(creatore);
	}
	
	public String getNome(){
		return nome;
	}
	
	public Utente getCreatore(){
		return creatore;
	}
	
	public Boolean partecipante(String utente){

		for (Utente u: partecipanti)
			if (utente.equals(u.getnome()))
				return true;
			
		return false;
	}
	
	public ArrayList<Utente> partecipanti(){
		return partecipanti;
	}
	
	public void addpartecipante(Utente u){
		partecipanti.add(u);
	}

	public void removepartecipante(Utente u){
		partecipanti.remove(u);
	}
	
	public String getIP(){
		return IP;
	}
	
	public void setIP(String IP){
		this.IP=IP;
	}
	
	public void disabilita(ServerImpl server){
		
		ArrayList<String> s = new ArrayList<String>();
		
		for (Utente u: partecipanti)
			if (u!=creatore)
				s.add(u.getnome());
		try {
			server.chiudichat(this.nome, this.IP, s);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}	
}
