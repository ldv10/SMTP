import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Pattern;

class SMTPHandler implements Runnable
{
	private Socket connection;
	private BufferedReader input;
	private DataOutputStream output;
	private Thread whoami;
	private Boolean heloOK = false, fromOK = false, rcptOK = false, dataOK = false, breakOK = false, quitOK = false;
	private String dataMemory = "", fromMemory = "", toMemory = "";
	private String emailRegex = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?";
	private static final String EMPTY_STRING = "";
	private DatabaseConnection db;
	
	public SMTPHandler(Socket connection) throws Exception
	{
		this.connection = connection;
		this.input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		this.output = new DataOutputStream(connection.getOutputStream());
		this.whoami = Thread.currentThread();
	}	
	
	@Override
	public void run() 
	{
		writeSocket("200 Service ready\n");
		while(!quitOK && whoami.isAlive())
		{
			String currentCommand = readSocket();
			if(!breakOK && !dataOK && !rcptOK && !fromOK && !heloOK)
			{
				this.heloOK = processHelo(currentCommand);
			} 
			else if (!breakOK && !dataOK && !rcptOK && !fromOK && heloOK)
			{
				this.fromOK = processMailFrom(currentCommand);
			}
			else if (!breakOK && !dataOK && !rcptOK && fromOK && heloOK)
			{
				this.rcptOK = processRcptTo(currentCommand);
			}
			else if (!breakOK && !dataOK && rcptOK && fromOK && heloOK)
			{
				this.dataOK = processData(currentCommand);
			}
			else if (!breakOK && dataOK && rcptOK && fromOK && heloOK)
			{
				processBreak(currentCommand);
			} else {
				if(!processQuit(currentCommand))
				{
					writeSocket("500 Syntax error, command unrecognized\n");
				}
			}
	  }

	  closeSocket();
	}

	private boolean processHelo(String data)
	{
		if(Pattern.matches("(HELO|EHLO)\\s+".concat(emailRegex), data))
		{	
			writeSocket("250 Hello, please to meet you\n");
			return true;
		}
		else if(processQuit(data))
		{
			return false;
		}
		writeSocket("500 Syntax error, command unrecognized\n");
		return false;		
	}

	private boolean processMailFrom(String data)
	{
		if(Pattern.matches("MAIL\\s+FROM:\\s+<".concat(emailRegex).concat(">"), data))
		{
			String email = parseEmail(data);
			this.fromMemory = email;
			writeSocket("250 OK\n");
			return true;
		}
		else if(processQuit(data))
		{
			return false;
		}
		writeSocket("500 Syntax error, command unrecognized\n");
		return false;
	}

	private boolean processRcptTo(String data)
	{			
		if(Pattern.matches("RCPT\\s+TO:\\s+<".concat(emailRegex).concat(">"), data))
		{
			String email = parseEmail(data);
			this.toMemory = email;
			writeSocket("250 OK\n");
			return true;											
		} 
		else if(processQuit(data))
		{
			return false;
		}
		writeSocket("500 Syntax error, command unrecognized\n");
		return false;		
	}
	
	private boolean processData(String data)
	{
		if(Pattern.matches("DATA|data", data))
		{
			writeSocket("354 End data with <CR><LF>.<CR><LF>\n");
			return true;
		}
		else if(processQuit(data))
		{
			return false;
		}
		writeSocket("500 Syntax error, command unrecognized\n");
		return false;
	}

	private void processBreak(String data)
	{
		String temp = data;
		while(!breakOK)
		{
			this.dataMemory = this.dataMemory.concat(" ").concat(temp);
			temp = readSocket();
			this.breakOK = temp.equals(".");
			if(breakOK)
			{
				System.out.println(String.valueOf(whoami.getId()).concat(" received end of data"));	
			}
		}

		this.db = new DatabaseConnection("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/mailserver", "postgres", "");

		if(this.db.isConnected())
		{
			if(this.db.executeUpdate(String.format("INSERT INTO public.mails (\"from\", \"to\", \"data\") VALUES ('%s', '%s', '%s')", this.fromMemory, this.toMemory, this.dataMemory)))
			{
				writeSocket("250 OK\n");
			}
			else
			{
				writeSocket("421 Service not available, closing transmission channel\n");		
			}
			this.db.closeConnection();
		}
		else
		{
			writeSocket("421 Service not available, closing transmission channel\n");
		}
	}

	private boolean processQuit(String data)
	{
		this.quitOK = Pattern.matches("QUIT|quit", data);
		if(this.quitOK)
		{
			writeSocket("221 Bye\n");
			return true;
		}

		return false;
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

	private String parseEmail(String data)
	{
		String result = "";
		try
		{
			String[] parsedBySpace = data.split("\\s+");
			result = parsedBySpace[2].substring(1, parsedBySpace[2].length()-1);
		}
		catch(Exception e)
		{
			System.err.println(String.valueOf(whoami.getId()).concat(": Caught Exception: ").concat(e.getMessage()));	
		}
		return result;
	}
}