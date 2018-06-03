package LatoClient;
import java.rmi.*;

import LatoClient.NotifyEventInterface;
public interface ServerInterface extends Remote
 {
 /* registrazione per la callback */
 public void registerForCallback (NotifyEventInterface ClientInterface) throws RemoteException;
 /* cancella registrazione per la callback */
 public void unregisterForCallback (NotifyEventInterface ClientInterface) throws RemoteException;
 
 
 }
