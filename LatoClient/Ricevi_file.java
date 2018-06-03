package LatoClient;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.*;

/* Thread che accetta le connessioni relative ai file */
public class Ricevi_file implements Runnable {

	int porta;
	JFrame frame;
	ServerSocketChannel serverChannel_file;
	
	public Ricevi_file(ServerSocketChannel serverChannel_file, JFrame frame){
		this.serverChannel_file=serverChannel_file;
		this.frame=frame;
	}
	
	
	@Override
	public void run() {
		
		File file;
		SocketChannel socket = null;
		 
		 while (true){
				
				try {
					socket = serverChannel_file.accept();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
				String path = System.getProperty("user.home") + "/Downloads/" + "SOCIAL-GOSSIP " + timeStamp ;
			 
				file = new File(path);

				FileChannel out=null;
				
				try {
					out = FileChannel.open(Paths.get(path),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
				} catch (IOException | NullPointerException e) {
						e.printStackTrace();
				}
					
				ByteBuffer buffer = ByteBuffer.allocateDirect(64);
				int letti=0;
				Boolean stop=false;	
				
				try {
					while (socket.read(buffer)!=-1)
					{ 
						buffer.flip();
						
						while (buffer.hasRemaining())
								out.write(buffer);
							
						buffer.clear();
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				try {
					out.close();
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				JOptionPane.showMessageDialog(frame, "Hai appena ricevuto un file, si trova nella tua cartella download\n");
			 }
	}
	
}
