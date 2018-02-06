import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

final class handler implements Runnable{

	public Socket conexion;
	public BufferedReader input;
	public DataOutputStream output;
	public DataInputStream in;
	
	boolean heloOK = false, fromOK = false, rcptOK = false,
			dataOK = false, breakOK = false, quitOK = false;
	
	String uEmisor, dEmisor, domUsu="", contCorreo;
	
	public handler(Socket conexion) throws IOException
	{

		this.conexion = conexion;
		input = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
		output = new DataOutputStream(conexion.getOutputStream());
		in = new DataInputStream(conexion.getInputStream());	
		System.out.println("200 Service ready\n");
	}	
	
	@Override
	public void run() {
		boolean exit = false;
		while(!exit)
		{				
				try {
					lector(); 
				} catch(Exception e)
				{
					exit=true;
				}				
				
				String comandos = "";
				
				try{
					comandos = input.readLine();
				}catch(Exception e)
				{
					e.printStackTrace();
					exit = true;
				}

			    System.out.println("Comand: "+comandos);
			    boolean valido = false;

			    valido = Pattern.matches("(QUIT)|(quit)", comandos);
			    if(valido)
			    {
			    	exit = true;
			    	System.out.println("221 - Bye \n");
			    }
			    else
			    { 	
			    	if(!heloOK)
				    {
				    	valido = processHelo(comandos);				    	
				    }
				    else
				    {
				    	if(!fromOK)
				    	{
				    		valido = processMailFrom(comandos);
				    	}
				    	else
				    	{
				    		if(rcptOK == false && dataOK == false)
				    		{
				    			valido = processRcptTo(comandos);	
				    		}
				    		else if(rcptOK == true && dataOK == false)
				    		{
				    			valido = processData(comandos);				    			
				    			if(!valido)
				    			{
				    				valido = processRcptTo(comandos);				    				
				    			}

				    		}
				    		else if(rcptOK == dataOK == true)
				    		{			    							    				
			    				boolean temp = Pattern.matches("\\.", comandos);
			    				if(temp)
			    				{
			    					breakOK = true;
			    					System.out.println("Correo: "+contCorreo);

			    					System.out.println("250 - OK \n");

			    					fromOK = false;
			    					rcptOK = false;
			    					dataOK = false;
			    					breakOK = false;
			    					uEmisor = "";
			    					dEmisor = "";
			    					contCorreo = "";
			    								    					
			    				}
			    				else
			    				{
			    					//System.out.println("Leyendo Data "+ comandos);
			    					valido = processDataContent(comandos); //!
			    				}							    			
				    		}
				    		else
				    		{
				    			System.out.println(".");
				    		}			    		
				    	}			    	
				    }
			    }				    					    
		}
		System.out.println("221 Service closing transmission channel");
	}

public void lector()
	{
		try {
			InputStreamReader input = new InputStreamReader(this.conexion.getInputStream());
			BufferedReader reader = new BufferedReader(input);
			this.input = reader;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public boolean processDataContent(String data)
	{
		try{
			contCorreo += data;
			//System.out.println("> "+contCorreo);
			//System.out.println("200 0k - Go on \n");
			return true;
		} catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}		
	}
	
	public boolean processData(String data)
	{
		boolean temp;			
		
		temp = Pattern.matches("(DATA)|(data)|(ladata)", data);
		
		if(temp)
		{		
			System.out.println("354 Start mail input; end with <CRLF>.<CRLF>\n");
			dataOK = true;
			return true;
		}
		return false;	
	}
	
	public boolean processRcptTo(String data)
	{
		String response = "500 Syntax error, command unrecognised \n";
		boolean temp;			
		temp = Pattern.matches("(RCPT)\\s+(TO:?)\\s+(.)+", data);
		
		if(temp)
		{

			String[] correoRecipiente = data.split("\\s+");
			String nombreRecipiente = correoRecipiente[2];
			String usuario, dominio;
			String[] formatoTemporal = nombreRecipiente.split("@");
			usuario = formatoTemporal[0];
			dominio = formatoTemporal[1];
			System.out.println(usuario+" @ "+dominio);			
			System.out.println("250 Requested mail action okay, completed \n");
			rcptOK = true;
					
			return true;											
		}
		else
		{
			System.out.println("500 Syntax error, command unrecognised \n");				
			return false;
		}		
	}
	
	public boolean processMailFrom(String data)
	{
		String response = "501 Syntax error in parameters or arguments \n";
		boolean temp;		
		temp = Pattern.matches("(MAIL)\\s+(FROM:?)\\s+(.)+", data);
		
		if(temp)
		{
			String[] correoUsuario = data.split("\\s+");
			String nombreAutor = correoUsuario[2];
			temp = Pattern.matches("<(.)+@(.)+>", nombreAutor);
			
			if(temp)
			{
				String[] formatoTemporal = nombreAutor.split("@");
				uEmisor = formatoTemporal[0];
				dEmisor = formatoTemporal[1];
				System.out.println(uEmisor+" @ "+dEmisor);

				if(!dEmisor.equals(domUsu+">"))
				{					
					System.out.println(response);					
					return false;					
				}		
								
				System.out.println("250 OK \n");					
				fromOK = true;
				return true;
			}
			else
			{
				System.out.println("500 Syntax error, command unrecognised \n");				
				return false;
			}			
		}
		else
		{
			System.out.println(response);
			return false;
		}
	}

	public boolean processHelo(String data)
	{
		String response = "500 Syntax error, command unrecognised \n";
		boolean temp;
		temp = Pattern.matches("(HELO|EHLO)(\\s)+(\\w)+.com", data);
		
		if(temp)
		{

			String[] correoUsuario = data.split("\\s+");			
			if(correoUsuario.length > 1)
			{
				String nombreDominio = correoUsuario[1]; 
				System.out.println(nombreDominio);
				System.out.println("250 OK\n");
				domUsu = nombreDominio; 
			}
			else
			{
				System.out.println("250 OK\n");
			}			
			heloOK = true;	
			return true;
		}	
		else
		{
			System.out.println(response);
			return false;
		}		
	}	
}