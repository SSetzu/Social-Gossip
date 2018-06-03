package LatoServer;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/* Main thread del server, fa partire i thread Service_message, Service_chatroom,Service_online  
 * accetta le connessioni di controllo (porta 5000) e le passa ad un threadpool di service */
public class Server {
	
	/* hashmap concorrente che contiene gli utenti e le chatroom */
	private static ConcurrentHashMap<String,Utente> utenti;
	private static ConcurrentHashMap<String,ChatRoom> chatroom;
	
	private static ThreadPoolExecutor executor;
	
	public static void main(String[] args){
		
		executor=new ThreadPoolExecutor(128,256, 1,TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(512));
		int port = 5000;
		utenti=new ConcurrentHashMap<String,Utente>();
		chatroom=new ConcurrentHashMap<String,ChatRoom>();
		
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException | NullPointerException e1 ) {
			e1.printStackTrace();
		}
		
		
		
		ServerImpl server = null;
		try {
			server = new ServerImpl( );
			ServerInterface stub=(ServerInterface) UnicastRemoteObject.exportObject (server,39000);
			String name = "Server";
			LocateRegistry.createRegistry(1099);
			Registry registry=LocateRegistry.getRegistry(1099);
			registry.bind (name, stub);

		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (AlreadyBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		Service_message s = new Service_message(utenti);
		Thread t1 = new Thread(s);
		t1.start();
		
		Service_chatroom s1 = new Service_chatroom(chatroom);
		Thread t2 = new Thread(s1);
		t2.start();
		
		Service_online s3 = new Service_online(utenti);
		Thread t3 = new Thread(s3);
		t3.start();
		
			
		while(true){
			try {
				Socket socket = serverSocket.accept();
				
				Service service = new Service(socket, utenti, chatroom, server);
				executor.execute(service);
					
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}	
	
}
