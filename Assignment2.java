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

    //Everything that interacts with the database goes here
    try {
      Class.forName("com.mysql.jdbc.Driver");
      String dburl = connectProps.getProperty("dburl");
      String username = connectProps.getProperty("user");
      conn = DriverManager.getConnection(dburl, connectProps);
      System.out.printf("Database connection %s %s established.%n",
        dburl, username);

      conn.close();
    } catch (SQLException ex) {
      System.out.printf("SQLException: %s%nSQLState: %s%nVendor Error: %s%n",
        ex.getMessage(), ex.getSQLState(), ex.getErrorCode());
    }
  }
/*
  static void showCompanies() throws SQLException {
    Statement stmt = conn.createStatement();
    ResultSet results = stmt.executeQuery("select Ticker, Name from Company");

    while (results.next()) {
      System.out.printf("%5s, %s%n",
        results.getString("Ticker"), results.getString("Name"));
    }

    stmt.close();
  }
  */
/*
  static void showTickerDay(String ticker, String date) throws SQLException {
    PreparedStatement pstmt = conn.PreparedStatement(
      "select OpenPrice, ClosePrice, HighPrice, LowPrice " +
      " from PriceVolume " +
      " where Ticker = ? and TransDate = ?");
    pstmt.setString(1, ticker);
    pstmt.setString(2, date);
    ResultSet rs = pstmt.executeQuery();

    if (rs.next()) {
      System.out.printf("Open: %.2f, High: %.2f, Low: %.2f, Close: %.2f%n",
        rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4));
    } else {
      System.out.printf("Ticker %s, Date %s not found.%n, ticker, date");
    }
    pstmt.close();
  }
*/
}
