package LatoClient;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class InviaFile implements Runnable {

	/* IP e porta del destinatario */
	private String IP;
	private int porta;
	
	/* percorso assoluto del file */
	private String path;
	
	private JFrame frame;
	
	/* Thread che si occupa di aprire il file e di inviare, utilizzando NIO, il file al 
	 * destinatario */
	public InviaFile(String IP,int porta, String path, JFrame frame){
		this.IP=IP;
		this.path=path;
		this.frame=frame;
		this.porta=porta;
	}
	
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		SocketAddress address = new InetSocketAddress(IP, porta);
		SocketChannel socketChannel = null;
		
		try {
			socketChannel = SocketChannel.open();
			socketChannel.connect(address);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(frame, "Non è possibile inviare il file all'utente selezionato\n");
			e.printStackTrace();
		}
		
		File file = new File(path);
		
		 if(file.exists() && file.isFile()){
			FileChannel in=null;
			try {
				in = FileChannel.open(Paths.get(path),StandardOpenOption.READ);
			} catch (IOException | NullPointerException e) {
				JOptionPane.showMessageDialog(frame, "Il file "+ path+ " non esiste\n");
				e.printStackTrace();
			}
			
			ByteBuffer buffer = ByteBuffer.allocateDirect(64);
			int letti=0;
			Boolean stop=false;		
			
			while (!stop)
			{ 
				int bytesRead = 0;
				try {
					bytesRead = in.read(buffer);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (bytesRead==-1)
					stop=true;
				
				buffer.flip();
				
				while (buffer.hasRemaining())
					try {
						socketChannel.write(buffer);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				buffer.clear();
			 }
			
			try {
				socketChannel.close();
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
			JOptionPane.showMessageDialog(frame, "Il file "+ path+ " non esiste\n");
		 
		 

	}
}
