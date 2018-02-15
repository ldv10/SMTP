import org.postgresql.Driver;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

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
    System.out.println("Update: ".concat(query));
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

  public ResultSet executeQuery(String query)
  {
    Statement stmt = null;
    System.out.println("Select: ".concat(query));
    try
    {
      stmt = this.c.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      return rs;
    }
    catch(SQLException e)
    {
      System.err.println(e.getMessage());
      return null;
    }
  }

  public List<Map<String, Object>> resultSetToList(ResultSet rs)
  {
    if(rs != null)
    {
      try
      {
        ResultSetMetaData md = rs.getMetaData();
        Integer column_count = md.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        while(rs.next())
        {
          Map<String, Object> row = new HashMap<String, Object>(column_count);
          for(Integer i = 1; i <= column_count; ++i)
          {
            row.put(md.getColumnName(i), rs.getObject(i));
          }
          rows.add(row);
        }
        return rows;  
      }
      catch(SQLException e)
      {
        System.err.println(e.getMessage());
        return null;   
      }
    }
    return null;
  }
}