import org.postgresql.Driver;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseConnection
{
  private Connection c = null;

  public DatabaseConnection(String driver, String location, String user, String pass)
  {
    try
    {
      Class.forName(driver);
    }
    catch(Exception e)
    {
      System.err.println(e.getMessage());
    }

    try
    {
      c = DriverManager.getConnection(location, user, pass);
    }
    catch(SQLException e)
    {
      System.err.println(e.getMessage());
    }
  }

  public boolean isConnected()
  {
    return c != null;
  }

  public boolean closeConnection()
  {
    try
    {
      this.c.close();
      return true;
    }
    catch(SQLException e)
    {
      System.err.println(e.getMessage());
      return false;
    }
  }

  public boolean executeUpdate(String query)
  {
    Statement stmt = null;
    System.out.println("Update: " + query);
    try
    {
      stmt = this.c.createStatement();
      stmt.executeUpdate(query);
      return true;
    }
    catch(SQLException e)
    {
      System.err.println(e.getMessage());
      return false;
    }
  }
}