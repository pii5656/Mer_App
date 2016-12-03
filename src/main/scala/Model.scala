import slick.driver.MySQLDriver.api._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import org.joda.time.DateTime
import org.joda.time.format._
import org.joda.time.DateTimeZone

import com.github.tototoshi.slick.MySQLJodaSupport._

case class Seller(id: Int, name: String, good: Int)
case class Item(id: Int, name: String, seller_id: Int, status: String)
case class Deal(id: Int, item_id: Int, time: DateTime)

class BigSellers(tag: Tag) extends Table[Seller](tag, "BigSellers") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def good = column[Int]("good")
  def * = (id, name, good) <> (Seller.tupled, Seller.unapply)
}

class Items(tag: Tag) extends Table[Item](tag, "Items") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def sellerId = column[Int]("sellerId")
  def status = column[String]("status")
  def * = (id, name, sellerId, status) <> (Item.tupled, Item.unapply)
}

class Deals(tag: Tag) extends Table[Deal](tag, "Deals") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def itemId = column[Int]("itemId")
  def time = column[DateTime]("time")
  def * = (id, itemId, time) <> (Deal.tupled, Deal.unapply)
}

object Model {
  val db = Database.forURL("jdbc:mysql://localhost/mercari", driver="com.mysql.jdbc.Driver", user="mercari_user", password="mercari_pass")
  val query = sql"SELECT id, name FROM test".as[(Int, String)]
  val f:Future[Vector[(Int, String)]] = db.run(query)

}
