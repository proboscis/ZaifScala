package net.pocketengineer.zaifscala

import dispatch._
import Defaults._
import scala.concurrent.duration._
import scala.concurrent.Await
import play.api.libs.json._
import net.pocketengineer.zaifscala.model._

/**
 * Created by DaisukeShosaki on 15/06/28.
 */
class ZaifApiService(val KEY: String, val SECRET: String) {
  private val ZAIF_TIMEOUT = 1000
  private val ZAIF_URL = "https://api.zaif.jp/tapi"

  private def zaifSignature(param: Seq[(String, String)]): String = {
    import javax.crypto.Mac
    import javax.crypto.spec.SecretKeySpec
    import java.net.URLEncoder

    val message = param.map(p => p._1 + "=" + p._2).mkString("&")

    val sha512HMAC = Mac.getInstance("HmacSHA512")
    val secretKey = new SecretKeySpec(SECRET.getBytes(), "HmacSHA512")
    sha512HMAC.init(secretKey)

    val a = sha512HMAC.doFinal(message.getBytes())
    a.map("%02x".format(_)).mkString
  }

  private def getZaifPriveteRequest(nonce: String, method: String, param: List[(String, String)]) = {
    val request = url(ZAIF_URL)

    val requestParam = List(("nonce" -> nonce), ("method" -> method)) ::: param
    val sig = zaifSignature(requestParam)
    val key = Map("Key" -> KEY)
    val sign = Map("Sign" -> sig)

    (request <:< key <:< sign).POST
  }

  private def getZaifBalance(nonce: Long): JsValue = {
    val method = "get_info"
    val secretRequest = getZaifPriveteRequest(nonce.toString, method, List.empty)
      .addParameter("nonce", nonce.toString)
      .addParameter("method", method)
    val result = Await.result(Http(secretRequest OK as.String), ZAIF_TIMEOUT.milliseconds)
    val json = Json.parse(result)
    json
  }

  /**
   * 取引可能な資産残高
   * @param nonce
   * @return
   */
  def getBalanceFunds(nonce: Long): Balance = {
    val json: JsValue = getZaifBalance(nonce)
    Balance (
      jpy = (json \ "return" \ "funds" \ "jpy").as[Int],
      btc = (json \ "return" \ "funds" \ "btc").as[Float],
      mona = (json \ "return" \ "funds" \ "mona").as[Int]
    )
  }

  /**
   * 預けている資産合計
   * @param nonce
   * @return
   */
  def getBalanceDeposit(nonce: Long): Balance = {
    val json = getZaifBalance(nonce)
    Balance (
      jpy = (json \ "return" \ "deposit" \ "jpy").as[Int],
      btc = (json \ "return" \ "deposit" \ "btc").as[Float],
      mona = (json \ "return" \ "deposit" \ "mona").as[Int]
    )
  }

  /**
   * 発注しているオーダーを取得する
   * @param nonce
   * @return
   */
  def getOpenOrders(nonce: Long, currencyPair: CurrencyPair): List[OpenOrder] = {
    val method = "active_orders"
    val param = List("currency_pair" -> currencyPair.toString)
    val secretRequest = getZaifPriveteRequest(nonce.toString, method, param)
      .addParameter("nonce", nonce.toString)
      .addParameter("method", method)
      .addParameter("currency_pair", currencyPair.toString)
    val result = Await.result(Http(secretRequest OK as.String), ZAIF_TIMEOUT.milliseconds)
    val json = Json.parse(result)

    (json \ "return").as[JsObject].fieldSet.map {
      field =>
        OpenOrder(
          field._1.toLong, if((field._2 \ "action").as[String] == "bid") "buy" else "sell",
          (field._2 \ "price").as[Float], (field._2 \ "currency_pair").as[String],
          (field._2 \ "amount").as[Float], (field._2 \ "timestamp").as[String]
        )
    }.toList
  }

  /**
   * 取引注文
   * @param nonce
   * @param orderType BUY or SELL
   * @param price 価格
   * @param amount 注文量
   * @return
   */
  def newOrder(nonce: Long, currencyPair: CurrencyPair, orderType: Position, price: Float, amount: Float): Boolean = {
    val method = "trade"
    val order = orderType match {
      case BUY => "bid"
      case SELL => "ask"
    }
    val (priceString, amountString) = currencyPair match {
      case BTC_JPY => (price.toInt.toString, amount.toString)
      case MONA_JPY => (price.toString, amount.toInt.toString)
    }
    val param = List("currency_pair" -> currencyPair.toString, "action" -> order, "price" -> priceString, "amount" -> amountString)
    val secretRequest = getZaifPriveteRequest(nonce.toString, method, param)
      .addParameter("nonce", nonce.toString) //署名する際のパラメータの順番と、リクエストのパラメータの順番が違うと弾かれる
      .addParameter("method", method)
      .addParameter("currency_pair", currencyPair.toString)
      .addParameter("action", order)
      .addParameter("price", priceString)
      .addParameter("amount", amountString)
    val result = Await.result(Http(secretRequest OK as.String), ZAIF_TIMEOUT.milliseconds)
    val json = Json.parse(result)
    println(json)
    (json \ "success").as[Long] > 0
  }

  /**
   * 取引のキャンセル
   * @param nonce
   * @param id order id
   * @return
   */
  def cancelOrder(nonce: Long, id: Long): Boolean = {
    val method = "cancel_order"
    val param = List("order_id" -> id.toString)
    val secretRequest = getZaifPriveteRequest(nonce.toString, method, param)
      .addParameter("nonce", nonce.toString)
      .addParameter("method", method)
      .addParameter("order_id", id.toString)
    val result = Await.result(Http(secretRequest OK as.String), ZAIF_TIMEOUT.milliseconds)
    val json = Json.parse(result)

    (json \ "success").as[Long] > 0
  }

  /**
   * 板情報
   * @param currencyPair 通貨ペア
   * @return
   */
  def getOrderBook(currencyPair: CurrencyPair) = {
    val request = url("https://api.zaif.jp/api/1/depth/" + currencyPair.toString)
    val result = Await.result(Http(request OK as.String), ZAIF_TIMEOUT.milliseconds)
    val json = Json.parse(result)

    val asks = (json \ "asks").as[JsArray].value.map(a => a.as[JsArray].value.toList).toList
    val bids = (json \ "bids").as[JsArray].value.map(a => a.as[JsArray].value.toList).toList

    OrderBook (
      asks = asks.map {
        ask =>
          PriceVolume(price = ask(0).as[Int], volume = ask(1).as[Float])
      },
      bids = bids.map {
        bid =>
          PriceVolume(price = bid(0).as[Int], volume = bid(1).as[Float])
      }
    )
  }

  /**
   * ティッカー
   * @param pair 通貨ペア
   * @return
   */
  def getTicker(pair: CurrencyPair): Ticker = {
    val request = url("https://api.zaif.jp/api/1/ticker/" + pair.toString)
    val result = Await.result(Http(request OK as.String), ZAIF_TIMEOUT.milliseconds)
    val json = Json.parse(result)

    Ticker (
      last = (json \ "last").as[Float],
      bid = (json \ "bid").as[Float],
      ask = (json \ "ask").as[Float],
      high = (json \ "high").as[Float],
      low = (json \ "low").as[Float],
      volume = (json \ "volume").as[Long]
    )
  }
}
