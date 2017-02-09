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
      //Connect to the database
      Class.forName("com.mysql.jdbc.Driver");
      String dburl = connectProps.getProperty("dburl");
      String username = connectProps.getProperty("user");
      conn = DriverManager.getConnection(dburl, connectProps);
      System.out.printf("Database connection %s %s established.%n",
        dburl, username);
      //Get input from user
      Scanner sc = new Scanner(System.in);
      boolean continueLoop = true;

      while (continueLoop) {
        System.out.printf("Enter a ticker symbol [start/end dates]: ");
				String input = sc.nextLine();
				
				if (input.trim().length() > 0) {
					String[] inputArgs = input.split(" ");
					int numArgs = inputArgs.length;
					String ticker = inputArgs[0];
					String startDate;
					String endDate;	
					//System.out.println("NumArgs: " + numArgs);
					if (numArgs != 1 && numArgs != 3) {
						System.out.printf("Wrong number of arguments.%n");
					}	
					else {
					
					findCompanyName(ticker);						
					
						if (numArgs == 3) {
							startDate = inputArgs[1];
							endDate = inputArgs [2];
							showTickerDay(ticker, startDate);			
						}				
					}
				} else {
					continueLoop = false;
				}

      }


      //Close the connection
      conn.close();
      System.out.printf("Connection closed.%n");
    } catch (SQLException ex) {
      System.out.printf("SQLException: %s%nSQLState: %s%nVendor Error: %s%n",
        ex.getMessage(), ex.getSQLState(), ex.getErrorCode());
    }
  }

  //Print a list of all companies
  static void showCompanies() throws SQLException {
    Statement stmt = conn.createStatement();
    ResultSet results = stmt.executeQuery("select Ticker, Name from Company");

    while (results.next()) {
      System.out.printf("%5s, %s%n",
        results.getString("Ticker"), results.getString("Name"));
    }

    stmt.close();
  }

  //Retrieve information from PriceVolume
	static void showTickerDay(String ticker, String date) throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement(
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
      System.out.printf("Ticker %s, Date %s not found.%n", ticker, date);
    }
    pstmt.close();
  }

  //Find a company with a given ticker
  static void findCompanyName(String ticker) throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement(
      "select Name " +
			" from Company " + 
			" where Ticker = ?");
		pstmt.setString(1, ticker);
		ResultSet rs = pstmt.executeQuery();
		
		if (rs.next()) {
			System.out.printf(rs.getString(1) + "%n");
		} else {
			System.out.printf("%s not found in database.%n", ticker);
		} 
  }
	
	//Helper function to view dates of a given company
	static void showDates(String ticker) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement(
			"select TransDate from PriceVolume where Ticker = ?"
		);
		pstmt.setString(1, ticker);
		ResultSet rs = pstmt.executeQuery();

		while (rs.next()) {
			System.out.printf(rs.getString("TransDate") + "%n");
		}	
	}

}
