//Universidad del Valle de Guatemala
//Redes
//Leonel Guillen - 14451
//Diego Sosa - 14735

import java.net.ServerSocket;
import java.net.Socket;

public class SMTPServer {
	
	public static int PUERTO = 80; 
	
	public static void main(String args[]) throws Exception
	{

		System.out.println("Servidor levantado en el puerto: "+PUERTO);
		ServerSocket s = new ServerSocket(PUERTO);
		
		while(true)
		{				
			Socket conexion = s.accept(); 		
			handler request = new handler(conexion);
			Thread thread = new Thread(request);
			thread.start();
			
		}					
	}
}

	
	