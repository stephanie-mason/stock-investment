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
          if (numArgs != 1 && numArgs != 3) {
            System.out.printf("Wrong number of arguments.%n");
          }
          else {
/*******************************************************************************
This is where most of the interesting stuff happens
*******************************************************************************/
            findCompanyName(ticker);
            if (numArgs == 3) {
              startDate = inputArgs[1];
              endDate = inputArgs [2];
              runDates(ticker, startDate, endDate);
            }	else {
              runDates(ticker);
            }
/******************************************************************************/
          }
        } else {
          continueLoop = false;
        }
      }
      conn.close();
      System.out.printf("Connection closed.%n");
    } catch (SQLException ex) {
      System.out.printf("SQLException: %s%nSQLState: %s%nVendor Error: %s%n",
      ex.getMessage(), ex.getSQLState(), ex.getErrorCode());
      conn.close();
    }
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


/*******************************************************************************
runDates
Iterates through data  in reverse chronological order within a given date
range (or all of the data for a ticker if no date range is given)
*******************************************************************************/
  static void runDates(String ticker, String startDate, String endDate)
  throws SQLException {
    StockDay currDay  = null;
    StockDay prevDay = null;
    PreparedStatement pstmt = conn.prepareStatement(
    "select TransDate " +
    " from PriceVolume " +
    " where Ticker = ?  " +
    " order by TransDate DESC"
    );
    pstmt.setString(1, ticker);
    ResultSet rs = pstmt.executeQuery();

    boolean continueLoop = false;
    String currDate;
    while (rs.next()) {
      currDate = rs.getString("TransDate");
      if (currDate.equals(endDate) ||
      continueLoop == true) {
        currDay = makeStockDay(ticker, currDate);
        continueLoop = true;
      } if (currDate.equals(startDate)) {
        continueLoop = false;
      }
    }
    pstmt.close();
  }
  //This one shows all the dates if given only a ticker
  static void runDates(String ticker) throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement(
    "select TransDate " +
    " from PriceVolume " +
    " where Ticker = ?  " +
    " order by TransDate DESC"
    );
    pstmt.setString(1, ticker);
    ResultSet rs = pstmt.executeQuery();
    while(rs.next()) {
      makeStockDay(ticker, rs.getString("TransDate"));
    }
    pstmt.close();
  }

/*******************************************************************************
  doTheThings
*******************************************************************************/
//What are we doing here... we need to:
// Create a stock day for the current day
// run the split comparison against the previous day
// if there is a list, add it to the split list
// update the divisor
// at the end of everything print the split list
static StockDay makeStockDay(String ticker, String date)
throws SQLException {
  StockDay thisStockDay = null;
  PreparedStatement pstmt = conn.prepareStatement(
  "select * " +
  " from PriceVolume " +
  " where Ticker = ? and TransDate = ?");
  pstmt.setString(1, ticker);
  pstmt.setString(2, date);
  ResultSet rs = pstmt.executeQuery();

  if (rs.next()) {
    /*System.out.printf("Ticker: %s Date: %s Open: %.2f, High: %.2f, " +
    " Low: %.2f, Close: %.2f, Volume: %.2f, AdjustedClose: %.2f %n",
    rs.getString("Ticker"), rs.getString("TransDate"),
    rs.getDouble("OpenPrice"), rs.getDouble("HighPrice"),
    rs.getDouble("LowPrice"), rs.getDouble("ClosePrice"),
    rs.getDouble("Volume"), rs.getDouble("AdjustedClose"));*/

    double openingPrice = rs.getDouble("OpenPrice");
    double highPrice = rs.getDouble("HighPrice");
    double lowPrice = rs.getDouble("LowPrice");
    double closingPrice = rs.getDouble("ClosePrice");
    double volumeOfShares = rs.getDouble("Volume");
    double adjustedClosingPrice = rs.getDouble("AdjustedClose");
    thisStockDay = new StockDay(ticker, date, openingPrice, highPrice,
    lowPrice, closingPrice, volumeOfShares, adjustedClosingPrice);

  } else {
    System.out.printf("Ticker %s, Date %s not found.%n", ticker, date);
  }
  pstmt.close();

  return thisStockDay;
}

/*******************************************************************************
findSplits
Runs through data  in reverse chronological order
As it does, compares each successive day to the previous in order to check
for stock splits. If a split is found, the remaining days are updated to
reflect their actual value with the split applied.
*******************************************************************************/
  static void findSplits(String ticker) {

  }




}

/* Function Graveyard */
/*
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


//Print information from PriceVolume for a given ticker/date
// (Helper function)
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
*/
