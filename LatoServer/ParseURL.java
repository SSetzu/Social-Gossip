package LatoServer;

public class ParseURL {

	String s;
	
	public ParseURL(String s){
		this.s=s;
	}
	
	/* restituisce una stringa in cui gli spazi vengono sostituiti con %20 */
	public String Parse(){
		
		String[] str = s.split(" ");
		String stringa = new String(str[0]);
		
		for (int i=1; i<str.length; i++)
			stringa = stringa.concat("%20"+str[i]);
	
		return stringa;
	}	
		
		
}