
import java.net.ServerSocket;
import java.net.Socket;

public class SMTPClientConnect {
	
	public static int PUERTO = 80; 
	
	public static void main(String args[]) throws Exception
	{

		System.out.println("Conectado al puerto: "+PUERTO);
		//ServerSocket s = new ServerSocket(PUERTO);
		Socket clientSocket = null;
		clientSocket = new Socket("localhost",80);
		while(true)
		{				
			//Socket conexion = s.connect(); 
					
			//handler request = new handler(conexion);
			smtpClient client = new smtpClient(clientSocket);
			//Thread thread = new Thread(request);
			Thread tclient = new Thread(client);
			tclient.start();

			//tclient.start();
		}					
	}
}
