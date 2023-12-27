package services

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtJson}
import com.typesafe.config.ConfigFactory
import models.{RegisteredUser, User}
import org.mindrot.jbcrypt.BCrypt
import play.api.Configuration
import play.api.libs.json.*
import play.api.libs.json.Reads.*
import play.api.libs.functional.syntax.*

import java.io.{File, FileInputStream, FileOutputStream, InputStreamReader}
import java.time.Clock
import javax.inject.Inject
import scala.util.Success

class AuthService @Inject()(conf : Configuration) {
  implicit val clock: Clock = Clock.systemUTC
  private val secretKey = conf.get[String]("application.secret_key")
  private val algo = JwtAlgorithm.HS256
  private val dbPath = "./resources/userdb.json"
  implicit val registeredUserFormat: Format[RegisteredUser] = (
    (JsPath \ "user_id").format[Int] and
      (JsPath \ "username").format[String] and
      (JsPath \ "password").format[String]
    ) (RegisteredUser.apply, r => (r.id, r.username, r.password))

  def generateToken(userID: Int): String = {
    val claim = JwtClaim(Json.stringify(Json.obj("user_id" -> userID)))
      .issuedNow
      .expiresIn(3600)
    Jwt.encode(claim, secretKey, algo)
  }

  def refreshToken(token: String): String = {
    val claim = JwtJson.decode(token, secretKey, Seq(algo)).get
    val newClaim = claim.issuedNow.expiresIn(3600)
    JwtJson.encode(newClaim, secretKey, algo)
  }

  private def validateToken(token: String): Boolean = {
    println("validateToken")
    if(Jwt.isValid(token, secretKey, Seq(algo))) {
      println("token is valid")
      val claim = Jwt.decode(token, secretKey, Seq(algo))
      println(s"claim: $claim")
      claim match {
        case Success(claim) => {
          try {
            val json = Json.parse(claim.content)
            val userID = (json \ "user_id").as[Int]
            val usersDB = readDB
            println(s"json: $json")
            println(s"userID: $userID")
            getUserByID(userID, usersDB) match {
              case Some(_) => true
              case _ => false
            }
          } catch {
            case e: Exception => false
          }
        }
        case Failure(_) => false
      }
    } else {
      println("token is invalid")
      false
    }
  }

  private def readDB = {
    println("readDB")
    val file = File(dbPath)
    val inputStream = FileInputStream(file)
    try {
      Json.parse(inputStream).as[List[RegisteredUser]]
    } finally {
      inputStream.close()
    }
  }

  def hashPassword(user: User): User = {
    user.copy(password = BCrypt.hashpw(user.password, BCrypt.gensalt()))
  }

  def checkPassword(user: User, password: String): Boolean = {
    BCrypt.checkpw(password, user.password)
  }

  def saveUser(user: User, usersDB: List[RegisteredUser]): Unit = {
    println("saveUser")
    val newID = usersDB.length + 1
    val newUser = RegisteredUser(
      id = newID,
      username = user.username,
      password = hashPassword(user).password
    )
    println(newUser)
    val file = FileOutputStream(dbPath)
    val json = Json.toJson(usersDB.::(newUser))
    println(json)
    file.write(json.toString().getBytes)
    file.flush()
    file.close()
  }

  def getUser(user: User, db: List[RegisteredUser]): Option[RegisteredUser] = {
    db.find(_.username == user.username)
  }

  def loginUser(user: User) = {
    val usersDB = readDB
    val someUser = getUser(user, usersDB)
    someUser match {
      case Some(registeredUser) =>
        if (checkPassword(user, registeredUser.password)) {
          generateToken(registeredUser.id)
        } else {
          throw new Exception("Wrong password")
        }
      case None => throw new Exception("User not found")
    }
  }

  def registerUser(user: User): Unit = {
    val usersDB = readDB
    println(usersDB)
    val someUser = getUser(user, usersDB)
    someUser match {
      case Some(_) => throw new Exception("User already exists")
      case None => saveUser(user, usersDB)
    }
  }
}
