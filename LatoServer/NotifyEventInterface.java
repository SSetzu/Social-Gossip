package LatoServer;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface NotifyEventInterface extends java.rmi.Remote { 
	
	public void notifyFriendship(String m, String d) throws RemoteException;
	
	public void notifyGroup(String name_group, String IP, ArrayList<String> utenti) throws RemoteException;
	

}