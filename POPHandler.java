import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Pattern;

class POPHandler implements Runnable
{
  private Socket connection;
  private BufferedReader input;
  private DataOutputStream output;
  private Thread whoami;
  private Boolean quitOK = false, userOK = false;
  private String user = "";
  private String emailRegex = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?";
  private static final String EMPTY_STRING = "";
  private DatabaseConnection db;
  
  public POPHandler(Socket connection) throws Exception
  {
    this.connection = connection;
    this.input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    this.output = new DataOutputStream(connection.getOutputStream());
    this.whoami = Thread.currentThread();
  } 
  
  @Override
  public void run() 
  {
    writeSocket("+OK\n");
    while(!quitOK && whoami.isAlive())
    {
      String currentCommand = readSocket();

      if(!userOK)
      {
        this.userOK = processUser(currentCommand);
      }
      else
      {
        processCommand(currentCommand);
      }
    }
    closeSocket();
  }

  private boolean processUser(String data)
  {
    if(Pattern.matches("APOP\\s+".concat(emailRegex), data) || Pattern.matches("USER\\s+".concat(emailRegex), data))
    {
      this.user = parseEmail(data);
      writeSocket("+OK\n");
      return true;
    }
    else if(processQuit(data))
    {
      return false;
    }
    return false;
  }

  private boolean processQuit(String data)
  {
    this.quitOK = Pattern.matches("QUIT|quit", data);
    if(this.quitOK)
    {
      writeSocket("+OK\n");
      return true;
    }
    writeSocket("-ERR\n");
    return false;
  }

  private void processCommand(String data)
  {
    if(Pattern.matches("STAT", data))
    {
      writeSocket("+OK STAT\n");
    }
    else if(Pattern.matches("LIST", data))
    {
      writeSocket("+OK LIST\n");
    }
    else if(Pattern.matches("DELE\\s*[0-9]*", data))
    {
      writeSocket("+OK DELE\n");
    }
    else if(Pattern.matches("RETR\\s*[0-9]*", data)){
      writeSocket("+OK RETR\n");
    }
    else if(Pattern.matches("NOOP", data))
    {
      writeSocket("+OK\n");
    }
    else if(Pattern.matches("RSET", data))
    {
      writeSocket("+OK RSET\n");
    }
    else if(Pattern.matches("TOP\\s*[0-9]*", data))
    {
      writeSocket("+OK TOP\n");
    }
    else if(Pattern.matches("UIDL\\s*[0-9]*", data))
    {
      writeSocket("+OK UIDL\n");
    }
    else
    {
      processQuit(data);
    }
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
      result = parsedBySpace[1];
    }
    catch(Exception e)
    {
      System.err.println(String.valueOf(whoami.getId()).concat(": Caught Exception: ").concat(e.getMessage())); 
    }
    return result;
  }
}