import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.nio.charset.StandardCharsets;

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
    this.db = new DatabaseConnection("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/mailserver", "postgres", "");
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
    this.db.closeConnection();
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
      if(this.db.executeUpdate(String.format("DELETE FROM public.mails WHERE \"to\"='%s' AND \"delete\"=true", this.user)))
      {
        writeSocket("+OK\n");
      }
      else
      {
        writeSocket("-ERR\n");
        return false;
      }
      return true;
    }
    writeSocket("-ERR\n");
    return false;
  }

  private void processCommand(String data)
  {
    if(Pattern.matches("STAT", data))
    {
      List<Map<String, Object>> result = this.db.resultSetToList(this.db.executeQuery(String.format("SELECT * FROM public.mails WHERE public.mails.to='%s'", this.user)));
      Integer size_bytes = 0, mail_count = 0;
      if(result != null)
      {
        mail_count = result.size();
        List<Integer> mail_sizes = new ArrayList<Integer>(mail_count);  
        for(Integer i = 0; i < mail_count; i++)
        {
          size_bytes += String.valueOf(result.get(i).get("data")).getBytes(StandardCharsets.UTF_8).length;
        }
      }
      writeSocket("+OK ".concat(String.valueOf(mail_count)).concat(" ").concat(String.valueOf(size_bytes)).concat("\n"));
    }
    else if(Pattern.matches("LIST", data))
    {
      List<Map<String, Object>> result = this.db.resultSetToList(this.db.executeQuery(String.format("SELECT * FROM public.mails WHERE public.mails.to='%s'", this.user)));
      Integer size_bytes = 0, mail_count = 0;
      if(result != null)
      {
        mail_count = result.size();
        List<Integer> mail_sizes = new ArrayList<Integer>(mail_count);  
        for(Integer i = 0; i < mail_count; i++)
        {
          size_bytes += String.valueOf(result.get(i).get("data")).getBytes(StandardCharsets.UTF_8).length;
        }
      }
      
      writeSocket("+OK ".concat(String.valueOf(mail_count)).concat(" messages ").concat(String.valueOf(size_bytes)).concat("\n"));
      size_bytes = 0;
      mail_count = 0;
      if(result != null)
      {
        mail_count = result.size();
        List<Integer> mail_sizes = new ArrayList<Integer>(mail_count);  
        for(Integer i = 0; i < mail_count; i++)
        {
          String m_index = String.valueOf(i+1);
          String s_index = String.valueOf(String.valueOf(result.get(i).get("data")).getBytes(StandardCharsets.UTF_8).length);
          writeSocket(m_index.concat(" ").concat(s_index).concat("\n"));
        }
        writeSocket(".\n");
      }
    }
    else if(Pattern.matches("DELE\\s*[0-9]*", data))
    {
      String[] parsedBySpace = data.split("\\s+");
      List<Map<String, Object>> result = this.db.resultSetToList(this.db.executeQuery(String.format("SELECT * FROM public.mails WHERE public.mails.to='%s'", this.user)));
      if(result != null && parsedBySpace.length == 2)
      {
        Integer m_index = Integer.parseInt(parsedBySpace[1]);
        String id_del = String.valueOf(result.get(m_index-1).get("id"));
        if(this.db.executeUpdate(String.format("UPDATE public.mails SET \"delete\" = true WHERE \"to\"='%s' AND \"id\"='%s'", this.user, id_del)))
        {
          writeSocket("+OK message ".concat(String.valueOf(m_index)).concat(" deleted\n"));
        }
        else
        {
          writeSocket("-ERR\n");  
        }
      } else {
        writeSocket("-ERR\n");
      }
    }
    else if(Pattern.matches("RETR\\s*[0-9]*", data))
    {
      String[] parsedBySpace = data.split("\\s+");
      List<Map<String, Object>> result = this.db.resultSetToList(this.db.executeQuery(String.format("SELECT * FROM public.mails WHERE public.mails.to='%s'", this.user)));
      if(result != null && parsedBySpace.length == 2)
      {
        Integer m_index = Integer.parseInt(parsedBySpace[1]);
        String id_ret = String.valueOf(result.get(m_index-1).get("data"));
        Integer size_bytes = id_ret.getBytes(StandardCharsets.UTF_8).length;
        writeSocket("+OK ".concat(String.valueOf(size_bytes)).concat(" octets\n"));
        writeSocket(id_ret.concat("\n"));
        writeSocket(".\n");
      } else {
        writeSocket("-ERR\n");
      }
    }
    else if(Pattern.matches("NOOP", data))
    {
      writeSocket("+OK\n");
    }
    else if(Pattern.matches("RSET", data))
    {
      if(this.db.executeUpdate(String.format("UPDATE public.mails SET \"delete\" = false WHERE \"to\"='%s'", this.user)))
      {
        writeSocket("+OK\n");
      }
      else
      {
        writeSocket("-ERR\n");
      }
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