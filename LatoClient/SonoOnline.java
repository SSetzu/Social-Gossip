package LatoClient;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/* Thread che accetta le connessioni di appello del Server per testare se l'utente è online */
public class SonoOnline implements Runnable {
	
	ServerSocket socket;
	
	public SonoOnline(ServerSocket socketonline){
		this.socket=socketonline;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		while (true){
			try {
				Socket timpano = socket.accept();
				timpano.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
