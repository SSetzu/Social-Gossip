package LatoServer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/* Thread che si occupa di accettare pacchetti sulla porta 5002 e di passare il messaggio
 * al Task_chatroom e di passare il Task_chatroom al pool di thread */
public class Service_chatroom implements Runnable {
	
	private ConcurrentHashMap<String,ChatRoom> chatroom;
	private ArrayBlockingQueue<Task_chatroom> coda_messaggi;
	private ThreadPoolExecutor executor;
	
	public Service_chatroom(ConcurrentHashMap<String,ChatRoom> chatroom){
		this.chatroom=chatroom;
		coda_messaggi = new ArrayBlockingQueue<Task_chatroom>(20);
		executor=new ThreadPoolExecutor(16,32,1,TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(64));
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		DatagramSocket serverSock=null;
		
		try {
			serverSock= new DatagramSocket(5002);
		} catch (SocketException | NullPointerException e) {
			e.printStackTrace();
		}
		
		while (true){
			byte[] buffer = new byte[2048];
			
			DatagramPacket receivedPacket = new DatagramPacket(buffer,buffer.length);
			
			
			try {
				serverSock.receive(receivedPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String line=new String(buffer);
			line=line.trim();
			
			System.out.println(line);
			Task_chatroom task = new Task_chatroom(line,chatroom);
				
			executor.execute(task);	 
			
		}	
	}
}