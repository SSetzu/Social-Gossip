package LatoClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JTextArea;

import org.omg.CORBA.portable.InputStream;

public class Ascolta_messaggi implements Runnable {

	JTextArea printArea;
	ConcurrentHashMap<String,Interfaccia_Chat> user_frames;
	String utente;
	String IP;
	
	ServerSocket Orecchio;
	
	public Ascolta_messaggi(ConcurrentHashMap<String,Interfaccia_Chat> user_frames, String utente,String IP, ServerSocket Orecchio){
		
		this.utente=utente;
		this.user_frames=user_frames;
		this.IP=IP;
		this.Orecchio=Orecchio;
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			
			while (true){
				Socket timpano = Orecchio.accept();
				BufferedReader  reader = new BufferedReader(new InputStreamReader(timpano.getInputStream()));
				
				String mittente=reader.readLine();
				String messaggio = reader.readLine();
				
				if (!user_frames.containsKey(mittente))
					user_frames.put(mittente,new Interfaccia_Chat(utente, mittente, user_frames,IP));
				
				user_frames.get(mittente).getTextField().append("<"+mittente+">: " + messaggio + "\n");
				timpano.close();
			}
			
			} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
