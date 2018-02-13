import java.net.ServerSocket;
import java.net.Socket;

public class ProtocolPOP implements Runnable
{
  ServerSocket server;

  public ProtocolPOP(Integer port)
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
      System.out.println("POP Protocol listening on port: ".concat(String.valueOf(port)));
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