package services

import models.{RegisteredUser, User}
import org.mindrot.jbcrypt.BCrypt
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtJson}
import play.api.Configuration
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import play.api.libs.json.Reads.*
import play.api.mvc.Session

import java.io.{File, FileInputStream, FileOutputStream}
import java.time.Clock
import javax.inject.Inject
import scala.util.{Success, Failure}

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

  private def generateToken(userID: Int): String = {
    val claim = JwtClaim(Json.stringify(Json.obj("user_id" -> userID)))
      .issuedNow
      .expiresIn(3600)
    Jwt.encode(claim, secretKey, algo)
  }

  def refreshToken(token: String): String = {
    val claim = Jwt.decode(token, secretKey, Seq(algo)).get
    val newClaim = claim.issuedNow.expiresIn(3600)
    Jwt.encode(newClaim, secretKey, algo)
  }

  def loggedIn(jwt: String): Boolean = {
    println("loggedIn")
    validateToken(jwt)
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

  private def hashPassword(user: User): User = {
    user.copy(password = BCrypt.hashpw(user.password, BCrypt.gensalt()))
  }

  private def checkPassword(user: User, password: String): Boolean = {
    BCrypt.checkpw(user.password, password)
  }

  private def saveUser(user: User, usersDB: List[RegisteredUser]): Unit = {
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
    file.write(Json.prettyPrint(json).getBytes())
    file.flush()
    file.close()
  }

  private def getUser(user: User, db: List[RegisteredUser]): Option[RegisteredUser] = {
    db.find(_.username == user.username)
  }

  private def getUserByID(id: Int, db: List[RegisteredUser]): Option[RegisteredUser] = {
    db.find(_.id == id)
  }

  def loginUser(user: User): String = {
    val usersDB = readDB
    val someUser = getUser(user, usersDB)
    someUser match {
      case Some(registeredUser) =>
        if (checkPassword(user, registeredUser.password)) {
          println("Generated token")
          generateToken(registeredUser.id)
        } else {
          throw new Exception("Wrong password")
        }
      case _ => throw new Exception("User not found")
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

