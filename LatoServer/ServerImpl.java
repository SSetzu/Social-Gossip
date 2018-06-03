package LatoServer;
import java.rmi.*; import java.rmi.server.*; import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import LatoClient.NotifyEventInterface;
public class ServerImpl extends RemoteObject implements ServerInterface
 { 	
	/* lista dei client registrati */
 	private LinkedBlockingQueue <NotifyEventInterface> clients;
	 
	 
	 
	 /* crea un nuovo servente */
	 public ServerImpl()throws RemoteException
	 {
		 super( );
		 clients = new LinkedBlockingQueue<NotifyEventInterface>( ); 
	 }
	
	 
	 public synchronized void registerForCallback (NotifyEventInterface ClientInterface) throws RemoteException
	{
		 if (!clients.contains(ClientInterface))
			 clients.add(ClientInterface);
			
		 
	}
		
	
	/* annulla registrazione per il callback */
	public synchronized void unregisterForCallback (NotifyEventInterface Client) throws RemoteException
	{
		
		if (clients.remove(Client))
			System.out.println("Client unregistered");
		else  
			System.out.println("Unable to unregisterclient."); 
	}
	
	
	private synchronized void doCallbacksFriend(String m , String d) throws RemoteException
	{ 
		
		Iterator i = clients.iterator( );
		while (i.hasNext()) {
			 NotifyEventInterface client = (NotifyEventInterface) i.next();
			 try {
				 client.notifyFriendship(m,d);
			 } catch (ConnectException e){
				 System.out.println("connessione RMI rifiutata");
				 unregisterForCallback(client);
			 }
		 }		
	}
	
	private synchronized void doCallbacksGroup(String m , String d, ArrayList<String> utenti) throws RemoteException
	{ 
		
		Iterator i = clients.iterator( );
		while (i.hasNext()) {
			 NotifyEventInterface client = (NotifyEventInterface) i.next();
			 try {
			 	client.notifyGroup(m,d, utenti);
			 } catch (ConnectException e){
				 System.out.println("connessione RMI rifiutata");
				 unregisterForCallback(client);
			 }			 
		 }		
	}

	public void update(String m, String d) throws RemoteException
	{
		doCallbacksFriend(m,d);
	}
	
	public void chiudichat(String m, String d, ArrayList<String> utenti) throws RemoteException
	{
		doCallbacksGroup(m,d, utenti);
	}	
}