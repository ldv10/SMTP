//Universidad del Valle de Guatemala
//Redes
//Leonel Guillen - 14451
//Diego Sosa - 14735

import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	
	public static int PORT = 8080; 
	
	public static void main(String args[]) throws Exception
	{

		System.out.println("Server listening on port: "+PORT);
		ServerSocket s = new ServerSocket(PORT);
		
		while(true)
		{				
			Socket connection = s.accept(); 		
			SMTPHandler request = new SMTPHandler(connection);
			Thread thread = new Thread(request);
			thread.start();
		}					
	}
}

	
	