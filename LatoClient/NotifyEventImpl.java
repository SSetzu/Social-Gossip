package LatoClient;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JTextArea;

public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {

	/* crea un nuovo callback client */
	JTextArea res;
	String utente;
	ConcurrentHashMap<String,Interfaccia_ChatRoom> chatroom_frames;
	
	
	public NotifyEventImpl(JTextArea res, String utente, ConcurrentHashMap<String,Interfaccia_ChatRoom> chatroom_frames) throws RemoteException
	{ 
		
		super( ); 
		this.res=res;
		this.utente=utente;
		this.chatroom_frames=chatroom_frames;
	
	}

	@Override
	public void notifyFriendship(String m, String d) throws RemoteException {
	// TODO Auto-generated method stub
		
		if (d.equals(utente))
			res.append("Tu e " + m + " siete diventati amici\n");
	} 
	
	public void notifyGroup(String name_group, String IP, ArrayList<String> utenti) throws RemoteException {
		
		if (utenti.contains(utente)){
			if (chatroom_frames.containsKey(name_group))
				chatroom_frames.get(name_group).exit();
			
			res.append("La chatroom "+ name_group + " è stata chiusa\n");
		}
	}

}