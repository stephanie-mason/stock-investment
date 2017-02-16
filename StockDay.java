/*============================================================================/*
| StockDay.java                                                                |
|                                                                              |
| Used by Assignment2.java                                                     |
|                                                                              |
| Creates a stock day object with:                                                 |
| * ticker symbol                                                              |
| * date                                                                       |
| * opening price                                                              |
| * high price                                                                 |
| * low price                                                                  |
| * closing price                                                              |
| * volume of shares traded                                                    |
| * adjusted closing price                                                     |
|                                                                              |
| by Stephanie Mason                                                           |
/*============================================================================*/

public class StockDay {
  String ticker;
  String date;
  double openingPrice;
  double highPrice;
  double lowPrice;
  double closingPrice;
  double volumeOfShares;
  double adjustedClosingPrice;

  public StockDay( String ticker, String date, double openingPrice,
  double highPrice, double lowPrice, double closingPrice, double volumeOfShares,
  double adjustedClosingPrice ) {
    this.ticker = ticker;
    this.date = date;
    this.openingPrice = openingPrice;
    this.highPrice = highPrice;
    this.lowPrice = lowPrice;
    this.closingPrice = closingPrice;
    this.volumeOfShares = volumeOfShares;
    this.adjustedClosingPrice = adjustedClosingPrice;
  }

  // Methods to return attributes
  public double getPriceDif() {
    double priceDif = (highPrice-lowPrice)/highPrice;
    return priceDif;
  }
  public String getTicker() {
    return this.ticker;
  }
  public String getDate() {
    return this.date;
  }
  public double getOpeningPrice() {
    return this.openingPrice;
  }
  public double getClosingPrice(){
    return this.closingPrice;
  }

  // Method to adjust the prices of stocks if a split has occured
  public void adjustPrices(double divisor) {
    this.openingPrice = this.openingPrice / divisor;
    this.highPrice = this.highPrice / divisor;
    this.lowPrice = this.lowPrice / divisor;
    this.closingPrice = this.closingPrice / divisor;
  }
}
