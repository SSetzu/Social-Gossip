package LatoServer;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class Utente {
	
	private String nome;
	private String password;
	private Boolean online;
	private ArrayList<Utente> amici;
	private ArrayList<ChatRoom> chatrooms;
	private String IP = null;
	private String lingua;
	private int porta_file;
	private int porta_messaggi;
	private int porta_online;
	
	public Utente(String nome, String password, String lingua){
		this.nome=nome;
		this.password=password;
		this.online=false;
		this.lingua=lingua;
		amici=new ArrayList<Utente>();
		chatrooms = new ArrayList<ChatRoom>();
	}
	
	public String getLingua(){
		return lingua;
	}
	
	public String getstato(){
		if (online)
			return "online";
		else
			return "offline";
	}
	
	public ArrayList<Utente> listfriend(){
		return amici;
	}
	
	public String getnome(){
		return nome;
	}
	
	public String getpassword(){
		return password;
	}
	
	public void SetOnline(){
		this.online=true;
	}

	
	public void SetOffline(){
		this.online=false;
	}
	
	public void friendship(Utente u, ServerImpl server){
		amici.add(u);
		
		if (this.getOnline())
			try {
				server.update(u.getnome(), this.getnome());
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public Boolean getOnline(){
		return online;
		
	}
	public String getIP(){
		return IP;
	}
	
	public void setIP(String IP){
		System.out.println(IP);
		this.IP=IP;
		
	}
	
	public void setPorta_messaggi(int porta){
		this.porta_messaggi=porta;
	}
	
	public int getPorta_messaggi(){
		return porta_messaggi;
	}
	
	public void setPorta_file(int porta){
		this.porta_file=porta;
	}
	
	public int getPorta_file(){
		return porta_file;
	}
	
	public void setPorta_online(int porta){
		this.porta_online=porta;
	}
	
	public int getPorta_online(){
		return porta_online;
	}
	
	public void removeGorup(ChatRoom s)
	{
		chatrooms.remove(s);
	}
	
	
	public void addGorup(ChatRoom s)
	{
		chatrooms.add(s);
	}
	
	public ArrayList<ChatRoom> getGroups()
	{
		return chatrooms;
	}
	
}