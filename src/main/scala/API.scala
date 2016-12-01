import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.{HttpURLConnection, URL}
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.util.AbstractMap.SimpleEntry
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection

import scala.io.Source._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import slick.driver.MySQLDriver.api._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object UserInfo {
//val USER_AGENT = "Mercari_r/511 (Android 23; ja; arm64-v8a,; samsung SC-02H Build/6.0.1)";
  val USER_AGENT = "Mercari_r/511 (Android 23; ja; arm64-v8a,; LGE Nexus 5X Build/6.0.1)"
  val XPLATFORM = "android"
  val XAPPVERSION ="511"
  //val ACCEPTENCODING = "gzip,deflate"
}

case class Response(json: String, error: Boolean)
case class RequestParam(name: String, value: String)
case class GlobalToken(global_access_token: String, global_refresh_token: String)
//case class ResultOfSearchBySellerId(result: String, data: (id: String, seller: ))

object MercariAPI {
implicit val formats = DefaultFormats
  // HTTPConnectionでリクエストを送る
  // url, param("uuid"=>uuid)
  def sendGETRequest(url: String, params: List[RequestParam] = List()): JValue = {
    val req_url = genRequestURLWithParam(url, params)
    val url_obj = new URL(req_url)
    val con = url_obj.openConnection.asInstanceOf[HttpURLConnection]
    con.setRequestMethod("GET")
    con.setRequestProperty("User-Agent", UserInfo.USER_AGENT);
    con.setRequestProperty("X-PLATFORM", UserInfo.XPLATFORM);
    con.setRequestProperty("X-APP-VERSION", UserInfo.XAPPVERSION);
    //con.setRequestProperty("Accept-Encoding", UserInfo.ACCEPTENCODING);
    // パラメータの追加
    for (p <- params) {
      con.setRequestProperty(p.name, p.value)
    }
    con.setRequestMethod("GET")
    // 結果の取得
    val inputStream = con.getInputStream
    val content = io.Source.fromInputStream(inputStream).mkString
    if (inputStream != null) inputStream.close
    parse(content)
  }
  // リクエストパラメータを含めたURLを生成
  def genRequestURLWithParam(url: String, params: List[RequestParam] = List()) = {
    if (params.length >= 1)
      url ++ "?" ++ params.head.name ++ "=" ++ params.head.value ++ params.tail.map{p => "&" ++ p.name ++ "=" ++ p.value}.foldLeft(""){(x,y) => x ++ y}
    else
      url
  }
  // アクセストークン生成
  def genAccessToken(): String = {
    val uuid = java.util.UUID.randomUUID.toString
    val access_url = "https://api.mercari.jp/auth/create_token"
    val content = sendGETRequest(access_url, List(RequestParam("uuid", uuid)))
    (content \\ "access_token").extract[String]
  }
  // グローバルトークン生成
  def genGlobalToken() = {
    val access_token = genAccessToken()
    println(access_token)
    val access_url = "https://api.mercari.jp/global_token/get"
    val content = sendGETRequest(access_url, List(RequestParam("_access_token", access_token)))
    // case classでパースして値を取り出す
    (content \\ "data").extract[GlobalToken].global_access_token
  }
}

object Search {
  implicit val formats = DefaultFormats
  def searchBySellerId(seller_id: String, access_token: String, global_token: String): Array[JObject] = {
    val base_url = "https://api.mercari.jp/items/get_items"
    val params = List(RequestParam("_access_token", access_token), RequestParam("_global_access_token", global_token), RequestParam("seller_id", seller_id))
    val req_url = MercariAPI.genRequestURLWithParam(base_url, params)
    val content = MercariAPI.sendGETRequest(req_url, params)
    println(pretty(content))
    (content \\ "data").extract[Array[JObject]]//.map{x => x \\ "name"}.map{println(_)}
  }
  def searchAll(access_token: String, global_token: String) = {
    val base_url = "https://api.mercari.jp/items/get_items"
    val params = List(RequestParam("_access_token", access_token), RequestParam("_global_access_token", global_token), RequestParam("type", "category"))
    val req_url = MercariAPI.genRequestURLWithParam(base_url, params)
    val content = MercariAPI.sendGETRequest(req_url, params)
    (content \\ "data").extract[Array[JObject]].map{x => x \\ "seller"}
  }
}

object User {
  implicit val formats = DefaultFormats
  def getUserProfile(access_token: String, global_token: String, user_id: String) = {
    val base_url = "https://api.mercari.jp/users/get_profile"
    val params = List(RequestParam("_access_token", access_token), RequestParam("_global_access_token", global_token), RequestParam("user_id", user_id))
    val req_url = MercariAPI.genRequestURLWithParam(base_url, params)
    val content = MercariAPI.sendGETRequest(req_url, params)
    content
  }
  def getRecentSellerId(access_token: String, global_token: String) = {
    val result = Search.searchAll(access_token, global_token)
    result.map{x => x \\ "id"}.map{x => pretty(x)}.toList
  }
  def getBigSellerIds(access_token: String, global_token: String, ids: List[String]) = {
    val results = ids.map{ i => User.getUserProfile(access_token, global_token, i)}
    results.filter{profile => (profile \\ "data" \\ "ratings" \\ "good").extract[Int] > 150}.map{profile => profile \\ "data" \\ "id"}.map{_.extract[Int]}
    //results
  }

}



object Test {
  import MercariAPI._
  import Search._
  import User._

  val access_token = genAccessToken
  val global_token = genGlobalToken
  def testSendGet = {
    sendGETRequest("https://item.mercari.com/jp/m697424512/")
  }
  def testGenReqURL() = {
    val uuid = java.util.UUID.randomUUID.toString
    val access_url = "https://api.mercari.jp/auth/create_token"
    genRequestURLWithParam(access_url, List(RequestParam("uuid", uuid)))
  }
  def testGenAT = {
    genAccessToken()
  }
  def testGenGT = {
    genGlobalToken
  }
  def testBySellerId = {
    searchBySellerId("345508455", access_token, global_token)
  }
  def testSearchAll = {
    searchAll(access_token, global_token).map{println(_)}
  }
  def testGetUser = {
    getUserProfile(access_token, global_token, "325094901")
  }
  def testRecentId = {
    getRecentSellerId(access_token, global_token)
  }
  def testGetBigSeller = {
    getBigSellerIds(access_token, global_token, getRecentSellerId(access_token, global_token))
  }
  def testDB {
    val db = Database.forURL("jdbc:mysql://localhost/mercari", driver="com.mysql.jdbc.Driver", user="mercari_user", password="mercari_pass")
    val query = sql"SELECT id, name FROM test".as[(Int, String)]
    val f:Future[Vector[(Int, String)]] = db.run(query)
    Await.result(f, Duration.Inf) foreach println
  }
}
