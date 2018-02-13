public class Server
{	
	public static int SMTP_PORT = 8080;
	public static int POP_PORT = 8081; 

	public static void main(String args[]) throws Exception
	{
		Thread s1 = new Thread(new ProtocolSMTP(SMTP_PORT));
  	Thread s2 = new Thread(new ProtocolPOP(POP_PORT));
  	s1.start();
   	s2.start();
	}
}