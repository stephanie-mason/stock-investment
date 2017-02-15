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
/*******************************************************************************
  This is where most of the interesting stuff happens
*******************************************************************************/
            findCompanyName(ticker);
            if (numArgs == 3) {
              startDate = inputArgs[1];
              endDate = inputArgs [2];
              setOfStockDays = runDates(ticker, startDate, endDate);
              investStrategy(setOfStockDays);
            }	else {
              setOfStockDays = runDates(ticker);
              investStrategy(setOfStockDays);
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
  * Adds each day to an arrayList - this list is returned
  * Checks for splits as it runs through the data and prints to screen
*******************************************************************************/
  static ArrayList<StockDay> runDates(String ticker, String ... dates)
  throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement(
    "select TransDate " +
    " from PriceVolume " +
    " where Ticker = ?  " +
    " order by TransDate DESC"
    );
    pstmt.setString(1, ticker);
    ResultSet rs = pstmt.executeQuery();
    boolean continueLoop = false;
    String prevDate = null;
    String currDate = null;
    ArrayList<StockDay> allDays = new ArrayList<StockDay>();
    StockDay currStockDay = null;
    StockDay prevStockDay = null;
    int numSplits = 0;
    int numTradeDays = 0;
    int divisor = 1;

    while (rs.next()) {
      prevDate = currDate;
      prevStockDay = currStockDay;
      currDate = rs.getString("TransDate");
      currStockDay = makeStockDay(ticker, currDate);
      currStockDay.adjustPrices(divisor);

      // Check for Splits for given input dates
      if (dates.length > 0) {
        if (currDate.equals(dates[1]) ||
        continueLoop == true) {

          String splitType = findSplits(currStockDay, prevStockDay, divisor);
          switch(splitType) {
            case "2:1":
              numSplits++;
              divisor = divisor*2;
              break;
            case "3:1":
              numSplits++;
              divisor = divisor*3;
              break;
            case "3:2":
              numSplits++;
              divisor = divisor*(3/2);
              break;
            case "none":
              break;
          }

          numTradeDays++;
          allDays.add(currStockDay);
          continueLoop = true;
        } if (currDate.equals(dates[0])) {
          continueLoop = false;
        }
      }
      // Check for Splits for all dates (if not given input dates)
      else {
        String splitType = findSplits(currStockDay, prevStockDay, divisor);
        switch(splitType) {
          case "2:1":
            numSplits++;
            divisor = divisor*2;
            break;
          case "3:1":
            numSplits++;
            divisor = divisor*3;
            break;
          case "3:2":
            numSplits++;
            divisor = divisor*(3/2);
            break;
          case "none":
            break;
        }

        numTradeDays++;
        allDays.add(currStockDay);
      }
    }

    System.out.printf("%d splits in %d trading days%n",
    numSplits, numTradeDays);
    pstmt.close();

    return allDays;
  }

/*******************************************************************************
  doTheThings
*******************************************************************************/
  //What are we doing here... we need to:
  // Create a stock day for the current day
  // run the split comparison against the previous day
  // if there is a split, print it
  // update the divisor
  // at the end of everything print the split list
  static String findSplits(StockDay currStockDay, StockDay prevStockDay,
    int adjust)
  throws SQLException {
    // Check for splits
    // Keep in mind that in this case, prevStockDay is the day on the previous
    // line, but because the days are listed in revers chronological order
    // prevStockDay is actually the following day
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
        //System.out.println(splitType + " split on " + currDate);
        String currDate = currStockDay.getDate();
        System.out.printf("%s split on %s %.2f -> %.2f %n",
        splitType, currDate,
        currClosePrice*adjust, prevOpenPricePrice*adjust);
      } else {
        splitType = "none";
      }
    }
    return splitType;
  }


/*******************************************************************************
  makeStockDay

  Create a StockDay object with a given set of data from PriceVolume relation
*******************************************************************************/
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

/*******************************************************************************
  investStrategy

  iterate through stock days in chronological order and execute stock strategy
  outlined in the function
*******************************************************************************/
  static void investStrategy(ArrayList<StockDay> setOfStockDays) {
    int dayCount = 1;
    int numTransactions = 0;
    double currAvg;
    double closing50DaySum = 0;
    double currCash = 0;
    double currShares = 0;

    for(int i = setOfStockDays.size()-1; i > 0; i--) {
      StockDay currStockDay = setOfStockDays.get(i);
      /*
          Maintain a moving average of the closing prices over a 50-day window. So for
          a given trading day d, the 50-day average is the average closing price for the 50
          previous trading days (days d-50 to d-1).

          If there are more than 51 days of data, compute 50-day average for the first
          fifty days. Proceeding forward from day 51 through the second-to-last trading day in
          the data set, execute the following strategy:
      */
      if (dayCount > 50) {
        if (dayCount == 51) System.out.printf("Executing investment strategy%n");
        double closeD = currStockDay.getClosingPrice();
        double openD = currStockDay.getOpeningPrice();
        double closeDminus1 = setOfStockDays.get(i+1).getOpeningPrice(); // previous day
        double closeDplus1 = setOfStockDays.get(i-1).getOpeningPrice(); // next day
        /*2.9.6 Regardless of trading activity, update 50-day average to reflect the average
        over the last 50 days, and continue with day d+1*/
        currAvg = closing50DaySum / 50;
        closing50DaySum -= setOfStockDays.get(i+50).getClosingPrice();

        /*2.9.2 (Buy criterion) If the close(d) < 50-day average and close(d) is less than
        open(d) by 3% or more (close(d) / open(d) <= 0.97), buy 100 shares of the stock
        at price open(d+1).*/
        /* 2.9.4 (Transaction Fee) For either a buy or sell transaction, cash is reduced by a
        transaction fee of $8.00.*/
        if (closeD < currAvg &&
        (closeD/openD) <= 0.97000001) {
          //System.out.println("Buying. Starting currShares/currCash: " + currShares + " /" + currCash);
          currShares += 100;
          currCash -= 100*closeDplus1;
          currCash -= 8; //transaction fee
          //System.out.println("Ending currShares/currCash: " + currShares + currCash);
          numTransactions++;
        }

        /*2.9.3 (Sell criterion) If the buy criterion is not met, then if shares >= 100 and
        open(d) > 50-day average and open(d) exceeds close(d-1) by 1% or more
        (open(d) / close(d-1) >= 1.01), sell 100 shares at price (open(d) + close(d))/2.*/
        /* 2.9.4 (Transaction Fee) For either a buy or sell transaction, cash is reduced by a
        transaction fee of $8.00. */
        else if (currShares >= 100 &&
        openD > currAvg &&
        (openD/closeDminus1) >= 1.00999999) {
          currShares -= 100;
          currCash += 100*((openD + closeD)/2);
          currCash -= 8; //transaction fee
          numTransactions++;
        }

        /*2.9.5 If neither the buy nor the sell criterion is met, do not trade on that day. */

        /*After having processed the data through the second-to-last day, if there are
        any shares remaining, on the last day add open(d) * shares remaining to cash to
        account for the value of those remaining shares (No transaction fee applies to this).*/
        if (i == 1) {
          openD = setOfStockDays.get(0).getOpeningPrice();
          currCash += openD*currShares;
        }
      }
      closing50DaySum += currStockDay.getClosingPrice();
      dayCount++;
    }
    System.out.println("day count: " + dayCount);
    if (dayCount > 50) {
      System.out.printf("Transactions executed: %d%n", numTransactions);
      System.out.printf("Net cash: %.2f%n", currCash);
    }

  }
}
