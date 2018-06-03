package LatoServer;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/* Thread che si occupa di accettare le connessioni sulla porta 5001 e di passare il socket ottenuto
 * al Task_message e di passare il Task_message al pool di thread */
public class Service_message implements Runnable {

	private ConcurrentHashMap<String,Utente> utenti;
	private ArrayBlockingQueue<Task_message> coda_messaggi;
	private ThreadPoolExecutor executor;
	
	
	public Service_message(ConcurrentHashMap<String , Utente> utenti){
		this.utenti=utenti;
		coda_messaggi = new ArrayBlockingQueue<Task_message>(20);
		executor=new ThreadPoolExecutor(16,32,1,TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(64));
	}

	public void run() {
		
		ServerSocket serverSocket = null;
		
		try {
			serverSocket = new ServerSocket(5001);
		} catch (IOException e) {
				// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		while (true){
				Socket socket=null;
				try {
					socket = serverSocket.accept();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Task_message task = new Task_message(socket,utenti);
				executor.execute(task);	  
		}
	}
}