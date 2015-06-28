package net.pocketengineer.zaifscala.model

trait Position {
  def toString: String
}

case object BUY extends Position {
  override def toString: String = "buy"
}

case object SELL extends Position {
  override def toString: String = "sell"
}

trait CurrencyPair {
  def toString: String
}

case object BTC_JPY extends CurrencyPair {
  override def toString: String = "btc_jpy"
}

case object MONA_JPY extends CurrencyPair {
  override def toString: String = "mona_jpy"
}

/**
 * ティッカー情報
 * @param last 最新の約定価格
 * @param bid Bid
 * @param ask Ask
 * @param high 高値
 * @param low 安値
 * @param volume 24H Volume
 */
case class Ticker(last: Float, bid: Float, ask: Float, high: Float, low: Float, volume: Long)

/**
 * 価格と、注文量
 * @param price 価格
 * @param volume 注文量
 */
case class PriceVolume(val price: Float, val volume: Float)

/**
 * オーダーブック　現在価格に近いほうが先頭
 * @param asks ex) 6,7,8,9
 * @param bids ex) 5,4,3,2
 */
case class OrderBook(val asks: List[PriceVolume], val bids: List[PriceVolume])

/**
 * アカウント残高
 * @param jpy 日本円
 * @param btc ビットコイン
 * @param mona MONA
 */
case class Balance
(
  jpy: Int,
  btc: Float,
  mona: Int
  )

/**
 * 未約定注文一覧
 * @param id
 * @param orderType buy or sell
 * @param rate
 * @param pair
 * @param pendingAmount 残り
 * @param date
 */
case class OpenOrder
(
  val id: Long,
  val orderType: String,
  val rate: Float,
  val pair: String,
  val pendingAmount: Float,
  val date: String
  )

