import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Pattern;


	

class smtpClient implements Runnable
{
	private Socket connection;
	private BufferedReader input;
	private DataOutputStream output;
	private Thread whoami;
	private Boolean heloOK = false, fromOK = false, rcptOK = false, dataOK = false, breakOK = false, quitOK = false;
	private String dataMemory = "", fromMemory = "", toMemory = "";
	private static final String EMPTY_STRING = "";
	//private DatabaseConnection db;
	private String dominio = "leonel@hotmail.com";
	private String ok = "250";
	private String mailFrom = "leonel@leonel.com";
	private String recipiente = "diego@sosa.com";
	private String dataMen = "Prueba :v";

	
	public smtpClient(Socket connection) throws Exception
	{
		this.connection = connection;
		this.input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		this.output = new DataOutputStream(connection.getOutputStream());
		this.whoami = Thread.currentThread();
		//Ignore la base de datos por ahora
		//this.db = new DatabaseConnection("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/mailserver", "postgres", "");
	}	
	
	@Override
	public void run() 
	{
	
		while(!quitOK && whoami.isAlive())
		{
			//writeSocket("HELO " + dominio + "\n");
			String currentCommand = readSocket();
			System.out.println("heloOK "+this.heloOK);
			System.out.println("fromOK "+this.fromOK);
			System.out.println("rcptOK "+this.rcptOK);
			System.out.println("dataOK "+this.dataOK);
			System.out.println("breakOK "+this.breakOK);
			if(!breakOK && !dataOK && !rcptOK && !fromOK && !heloOK)
			{
				System.out.println("heloOK");
				processHelo(currentCommand);
			} 
			else if (!breakOK && !dataOK && !rcptOK && !fromOK && heloOK)
			{
				System.out.println("fromOK");
				this.fromOK = processMailFrom(currentCommand);
			}
			else if (!breakOK && !dataOK && !rcptOK && fromOK && heloOK)
			{
				System.out.println("rptOK");
				this.rcptOK = processRcptTo(currentCommand);
			}
			else if (!breakOK && !dataOK && rcptOK && fromOK && heloOK)
			{
				System.out.println("dataOK");
				this.dataOK = processData(currentCommand);
			}
			else if (!breakOK && dataOK && rcptOK && fromOK && heloOK)
			{
				System.out.println("breakOK");
				processBreak(currentCommand);
			} else {
				if(!processQuit(currentCommand))
				{
					writeSocket("\n");
				}
			}
	  }
	  //this.db.closeConnection();
	  closeSocket();
	}
	//Envia el HELO
	private void processHelo(String data)
	{
		if (data.substring(0,3).equals("200")){
			writeSocket("HELO " + dominio + "\n");
			System.out.println(this.heloOK);
			this.heloOK = true;
			System.out.println(this.heloOK);	
		}
	}


	//Mail from
	private boolean processMailFrom(String data)
	{
			writeSocket("MAIL FROM: <" + mailFrom + "> \n");
			return true;
	}

	private boolean processRcptTo(String data)
	{			
			writeSocket("RCPT TO: " + recipiente + "\n");
			return true;											
	}
	
	private boolean processData(String data)
	{

			writeSocket("Data\n");
			writeSocket(dataMen+ "\n");
			writeSocket(".\n");
			return true;
	}

	private void processBreak(String data)
	{
		//Aqui no se si deberia ir algo porque lo usa en las condiciones de ProcessData pero ahi mande el punto, sino solo salta este metodo
	}

	private boolean processQuit(String data)
	{

			writeSocket("QUIT\n");
			return true;
	}

	private void writeSocket(String message)
	{
		try 
		{
			output.write(message.getBytes());
			System.out.println(String.valueOf(whoami.getId()).concat(" wrote: ").concat(message));
		} 
		catch(Exception e)
		{
			System.err.println(String.valueOf(whoami.getId()).concat(": Caught Exception: ").concat(e.getMessage()));
			this.quitOK = true;
		}
	}

	private String readSocket()
	{
		try
		{
			String result = this.input.readLine();
			if (result != null)
      {
      	System.out.println(String.valueOf(whoami.getId()).concat(" received: ").concat(result));
				return result;
      }
      this.quitOK = true;
			return EMPTY_STRING;
		}
		catch(Exception e)
		{
			System.err.println(String.valueOf(whoami.getId()).concat(": Caught Exception: ").concat(e.getMessage()));
			this.quitOK = true;
		}
		
		return EMPTY_STRING;
	}

	private void closeSocket()
	{
		try
		{
			this.connection.close();
		}
		catch(Exception e)
		{
			System.err.println(String.valueOf(whoami.getId()).concat(": Caught Exception: ").concat(e.getMessage()));
		}
	}

}
