ZaifScala
===

# Description
Zaif Api for Scala.

https://corp.zaif.jp/api-docs/

## Requirement
sbt

## Usage
    import net.pocketengineer.zaifscala.model._

    // For private api, there is a need to use while incrementing the value.
    val nonce = System.currentTimeMillis() / 1000

    val zaif = new ZaifApiService("YOUR-KEY-HERE", "YOUR-SECRET-HERE")

    // funds
    val funds: Balance = zaif.getBalanceFunds(nonce)

    // deposits
    val deposits: Balance = zaif.getBalanceDeposit(nonce)

    // open orders
    val openOrders: List[OpenOrder] = zaif.getOpenOrders(nonce, MONA_JPY) // BTC_JPY or MONA_JPY

    // new order
    val isSuccess = zaif.newOrder(nonce, MONA_JPY, BUY, 19F, 1F)

    // cancel order
    val isSuccess = zaif.cancelOrder(nonce, 2340307L)

    // order book
    val orderBook: OrderBook = zaif.getOrderBook(MONA_JPY)

    // ticker
    val ticker: Ticker = zaif.getTicker(BTC_JPY)

## Build
`sbt assembly`

## Licence
The MIT License

## Author
@tanapro
https://twitter.com/tanapro

