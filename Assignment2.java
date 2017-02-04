/*============================================================================/*
| Assignment2.java                                                             |
|                                                                              |
| CSCI 330 - Winter 2017                                                       |
|                                                                              |
| Stock Investment Strategy                                                    |
| by Stephanie Mason                                                           |
/*============================================================================*/

import java.util.Properties;
import java.util.Scanner;
import java.io.FileInputStream;
import java.sql.*;

class Assignment2 {
  static Connection conn = null;

  public static void main(String[] args) throws Exception {
    //Parameters for a 'connection object'
    String paramsFile = "ConnectionParameters.txt";
    if (args.length >= 1) {
      paramsFile = args[0];
    }

    Properties connectProps = new Properties();
    connectProps.load(new FileInputStream(paramsFile));

    try {
      Class.forName("com.mysql.jbdc.Driver");
      String dburl = connectProps.getProperty("dburl");
      String username = connectProps.getProperty("user");
      conn = DriverManager.getConnection(dburl, connectProps);
      System.out.printf("Database connection %s %s established.%n", dburl, username);
    } catch (SQLException ex) {
      System.out.printf("SQLException: %s%nSQLState: %s%nVendor Error: %s%n",
        ex.getMessage(), ex.getSQLState(), ex.getErrorCode());
    }
  }

}
