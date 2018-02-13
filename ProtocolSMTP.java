import java.net.ServerSocket;
import java.net.Socket;

public class ProtocolSMTP implements Runnable
{
  ServerSocket server;

  public ProtocolSMTP(Integer port) throws Exception
  {
    try
    {
      this.server = new ServerSocket(port);
    }
    catch(Exception e)
    {
      System.err.println(e.getMessage());
    }
    finally{
      System.out.println("SMTP Protocol listening on port: ".concat(String.valueOf(port)));
    }
  }

  @Override
  public void run()
  {
    while(true)
    {
      try
      {
        Socket connection = server.accept();
        SMTPHandler request = new SMTPHandler(connection);
        Thread thread = new Thread(request);
        thread.start();
      } 
      catch(Exception e)
      {
        System.err.println(e.getMessage());
      }
    }
  }
}