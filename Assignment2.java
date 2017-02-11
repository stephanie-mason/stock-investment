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

      //Get ticker/date input from user for as long as they want to give it
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
							showDates(ticker, startDate, endDate);
						}	else {
								showDates(ticker);
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
				conn.close();
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

  //Print information from PriceVolume
	static void showPriceVolume(String ticker, String date)
	throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement(
      "select * " +
      " from PriceVolume " +
      " where Ticker = ? and TransDate = ? order by TransDate DESC");
    pstmt.setString(1, ticker);
		pstmt.setString(2, date);
    ResultSet rs = pstmt.executeQuery();

    if (rs.next()) {
      System.out.printf("Ticker: %s Date: %s Open: %.2f, High: %.2f, " +
        " Low: %.2f, Close: %.2f, Volume: %.2f, AdjustedClose: %.2f %n",
        rs.getString("Ticker"), rs.getString("TransDate"),
        rs.getDouble("OpenPrice"), rs.getDouble("HighPrice"),
        rs.getDouble("LowPrice"), rs.getDouble("ClosePrice"),
				rs.getDouble("Volume"), rs.getDouble("AdjustedClose"));
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

	//Helper functions to view dates of a given company
  //This one does it if you have a specific start and end date
	static void showDates(String ticker, String startDate, String endDate)
	throws SQLException {
    ResultSet rs = getPriceVolume(ticker);

		boolean printDate = false;
		while (rs.next()) {
			if (rs.getString("TransDate").equals(endDate) ||
					rs.getString("TransDate") == "2014.08.18" ||
					printDate == true) {
				//System.out.printf(rs.getString("TransDate") + "%n");
        showPriceVolume(ticker, rs.getString("TransDate"));
        printDate = true;
			} if (rs.getString("TransDate").equals(startDate)) {
				printDate = false;
			}
		}
	}

  //This one shows all the dates if given only a ticker
  static void showDates(String ticker) throws SQLException {
    ResultSet rs = getPriceVolume(ticker);
    while(rs.next()) {
      showPriceVolume(ticker, rs.getString("TransDate"));
    }
  }

  //Get price volume data for a given ticker
  static ResultSet getPriceVolume(String ticker) throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement(
      "select TransDate " +
      " from PriceVolume " +
      " where Ticker = ?  " +
      " order by TransDate DESC"
    );
    pstmt.setString(1, ticker);
    ResultSet rs = pstmt.executeQuery();

    return rs;
  }

}
