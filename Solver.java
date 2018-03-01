import java.net.*;
import java.io.*;


public class Solver {

    public static void main(String[] args) {
    	try{
        InetAddress giriAddress = java.net.InetAddress.getByName("localhost");
        String address = giriAddress.getHostAddress();
        System.out.println(address);
    	}
    	
    	catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

}