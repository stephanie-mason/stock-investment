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
import java.util.ArrayList;
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
    ArrayList<StockDay> setOfStockDays = new ArrayList<StockDay>();

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

/*============================================================================/*
This is where the main functions get called
/*----------------------------------------------------------------------------*/
          if (findCompanyName(ticker)){
            if (numArgs == 3) {
              startDate = inputArgs[1];
              endDate = inputArgs [2];
              setOfStockDays = runDates(ticker, startDate, endDate);
              if(setOfStockDays.size() > 50)
                investStrategy(setOfStockDays);
              else System.out.println("Net cash: 0");
            }	else {
              setOfStockDays = runDates(ticker);
              if(setOfStockDays.size() > 50)
                investStrategy(setOfStockDays);
              else System.out.printf("Net cash: 0%n%n");
            }
        }
/*----------------------------------------------------------------------------*/

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

/*============================================================================/*
findCompanyName

Given a stock ticker, this function queries the company relation to obtain the
company name associated with that ticker and prints it to the screen. If the
ticker is not in the database an error message is printed instead.
/*============================================================================*/
static boolean findCompanyName(String ticker) throws SQLException {
  PreparedStatement pstmt = conn.prepareStatement(
  "select Name " +
  " from Company " +
  " where Ticker = ?");
  pstmt.setString(1, ticker);
  ResultSet rs = pstmt.executeQuery();

  if (rs.next()) {
    System.out.printf(rs.getString(1) + "%n");
    return true;
  } else {
    System.out.printf("%s not found in database.%n", ticker);
    return false;
  }
}


/*============================================================================/*
runDates

Queries the PriceVolume relation using a ticker and (optional) date range to
get the transaction dates for the given ticker.
Iterates through dates in reverse chronological order (or all of the data for
if no date range is given.) While it does this it also:

  * Adds each day to an arrayList - this list is returned
  * Checks for splits
  * Automatically updates any old dates with new prices based on splits
    (by maintaining a divisor)
/*============================================================================*/
static ArrayList<StockDay> runDates(String ticker, String ... dates)
throws SQLException {
  String startDate = null;
  String endDate = null;
  if (dates.length > 0) {
    startDate = dates[0];
    endDate = dates[1];
  }
	else {
  	startDate = "1970.01.01";
		endDate = "2017.12.31"; 
  }
  //Query database to get the transaction dates
  PreparedStatement pstmt = conn.prepareStatement(
  "select TransDate " +
  " from PriceVolume " +
  " where TransDate between ? and ? and Ticker = ?" +
  " order by TransDate DESC"
  );
  pstmt.setString(1, startDate);
	pstmt.setString(2, endDate);
	pstmt.setString(3, ticker);
  ResultSet rs = pstmt.executeQuery();

  //Variables for looping through data
  boolean runDate = false;
  ArrayList<StockDay> allDays = new ArrayList<StockDay>();
  StockDay currStockDay = null;
  StockDay prevStockDay = null;
  String prevDate = null;
  String currDate = null;
  int numSplits = 0;
  int numTradeDays = 0;
  double divisor = 1;


  while (rs.next()) {
    prevDate = currDate;
    currDate = rs.getString("TransDate");

    prevStockDay = currStockDay;
    currStockDay = makeStockDay(ticker, currDate);
    currStockDay.adjustPrices(divisor);

    // Check for Splits over the date range and update the divisor
      String splitType = findSplits(currStockDay, prevStockDay, divisor);
      switch(splitType) {
        case "2:1":
          currStockDay.adjustPrices(1/divisor);
          divisor = divisor*2;
          currStockDay.adjustPrices(divisor);
          numSplits++;
          break;
        case "3:1":
        currStockDay.adjustPrices(1/divisor);
        divisor = divisor*3;
        currStockDay.adjustPrices(divisor);
        numSplits++;
          break;
        case "3:2":
        currStockDay.adjustPrices(1/divisor);
        divisor = divisor*1.5;
        currStockDay.adjustPrices(divisor);
        numSplits++;
          break;
        case "none":
          break;
      }

      numTradeDays++;
      allDays.add(currStockDay);
    }

  System.out.printf("%d splits in %d trading days%n%n",
  numSplits, numTradeDays);
  pstmt.close();
  return allDays;
}


/*============================================================================/*
makeStockDay

Queries the PriceVolume relation and creates a StockDay object with the data
/*============================================================================*/
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


/*============================================================================/*
findSplits

Compares two stock days to see if a stock split occured. If one does occur,
prints the type of split, the date, and the before/after split opening prices.
Returns the type of split that occured (or "none")
/*============================================================================*/
static String findSplits(StockDay currStockDay, StockDay prevStockDay,
  double adjust)
throws SQLException {
  String splitType = "none";

  if (prevStockDay != null) {
    double currClosePrice = currStockDay.getClosingPrice();
    double prevOpenPricePrice = prevStockDay.getOpeningPrice();
    boolean didSplit = false;

    // 2:1 split
    if (Math.abs((currClosePrice/prevOpenPricePrice) - 2.0) < 0.20) {
      didSplit = true;
      splitType = "2:1";
    }
    // 3:1 split
    if (Math.abs((currClosePrice/prevOpenPricePrice) - 3.0) < 0.30) {
      didSplit = true;
      splitType = "3:1";
    }
    // 3:2 split
    if (Math.abs((currClosePrice/prevOpenPricePrice) - 1.5) < 0.15) {
      didSplit = true;
      splitType = "3:2";
    }

    if (didSplit == true) {
      String currDate = currStockDay.getDate();
      System.out.printf("%s split on %s %.2f -> %.2f %n",
      splitType, currDate,
      currClosePrice*adjust, prevOpenPricePrice*adjust);
    }
  }
  return splitType;
}


/*============================================================================/*
investStrategy

* Maintains a moving average of the 50 days prior to the current trading date
* Buys 100 shares at price open(d+1) if:
  * close(d) < 50-day average AND
  * close(d) is less than open(d) by 3% or more
* Sells 100 shares at price (open(d) + close(d))/2 if :
  * Buy criterion is not met
  * Shares >= 100 AND
  * open(d) > 50-day average AND
  * open(d) exceeds closde(d-1) by 10% or more
(Where d is the current trading day, d+1 is the next trading day, and d-1 is
the previous trading day; close() and open() refer to the closing and opening
prices for that day)
* There is an $8.00 transaction fee for all buy/sell transactions
* If neither buy nor sell criterion are met, do not trade on that day
/*============================================================================*/
static void investStrategy(ArrayList<StockDay> setOfStockDays) {
  int dayCount = 0;
  int numTransactions = 0;
  int currShares = 0;
  double currCash = 0;
  double currAvg;
  double closing50DaySum = 0;

  System.out.printf("Executing investment strategy%n");

  for(int i = setOfStockDays.size()-1; i > 0; i--) {
    dayCount++;
    StockDay currStockDay = setOfStockDays.get(i);

    if (dayCount > 50) {
      double openD = currStockDay.getOpeningPrice();
      double openDminus1 = setOfStockDays.get(i+1).getOpeningPrice();
      double openDplus1 = setOfStockDays.get(i-1).getOpeningPrice();
      double closeD = currStockDay.getClosingPrice();
      double closeDminus1 = setOfStockDays.get(i+1).getClosingPrice();
      double closeDplus1 = setOfStockDays.get(i-1).getClosingPrice();

      //Moving average is of 50 days prior to current trading day
      currAvg = closing50DaySum / 50;
      closing50DaySum -= setOfStockDays.get(i+50).getClosingPrice();

      //Buy criterion
      if (closeD < currAvg &&
      (closeD/openD) <= 0.97000001) {
        currShares += 100;
        currCash -= 100*openDplus1;
        currCash -= 8; //transaction fee
        numTransactions++;
      }
      //Sell criterion
      else if (currShares >= 100 &&
      openD > currAvg &&
      (openD/closeDminus1) >= 1.00999999) {
        currShares -= 100;
        currCash += 100*((openD + closeD)/2);
        currCash -= 8; //transaction fee
        numTransactions++;
      }

      //Final day, evaluate net worth based on current shares
      if (i == 1) {
        openD = setOfStockDays.get(0).getOpeningPrice();
        currCash += openD*currShares;
      }
    }

    closing50DaySum += currStockDay.getClosingPrice();
  }

  System.out.printf("Transactions executed: %d%n", numTransactions);
  System.out.printf("Net cash: %.2f%n%n", currCash);
}
}
