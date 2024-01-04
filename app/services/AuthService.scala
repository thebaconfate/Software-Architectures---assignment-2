package services


import models.{RegisteredUser, User}
import org.mindrot.jbcrypt.BCrypt
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import play.api.Configuration
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import play.api.libs.json.Reads.*
import play.api.mvc.RequestHeader

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.time.Clock
import javax.inject.Inject
import scala.util.{Failure, Success}

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
  val jwtKey = "jwt"
  val unAuthMsg = "You are not logged in, please login at /login"
  val imagePath = "./public/images"
  private def generateToken(username: String): String = {
    val claim = JwtClaim(Json.stringify(Json.obj("username" -> username)))
      .issuedNow
      .expiresIn(3600)
    Jwt.encode(claim, secretKey, algo)

  }
  

  private def validateToken(token: String): Boolean = {
    if(Jwt.isValid(token, secretKey, Seq(algo))) {
      val claim = Jwt.decode(token, secretKey, Seq(algo))
      claim match {
        case Success(claim) =>
          try {
            val json = Json.parse(claim.content)
            val username = (json \ "username").as[String]
            val usersDB = readDB
            getUserByUsername(username, usersDB) match {
              case Some(_) =>
                true
              case _ => false
            }
          } catch {
            case e: Exception => false
          }
        case Failure(_) => false
      }
    } else {
      false
    }
  }

  def isAuthenticated(request : RequestHeader): Boolean = {
    request.session.get(jwtKey) match {
      case Some(token) => validateToken(token)
      case None => false
    }
  }

  def getUsername(request: RequestHeader): Option[String] = {
    request.session.get(jwtKey) match {
      case Some(token) =>
        val claim = Jwt.decode(token, secretKey, Seq(algo))
        claim match {
          case Success(claim) =>
            try {
              val json = Json.parse(claim.content)
              val username = (json \ "username").as[String]
              val userDB = readDB
              getUserByUsername(username, userDB) match {
                case Some(user) => Some(user.username)
                case _ => None
              }
            } catch {
              case e: Exception => None
            }
          case Failure(_) => None
        }
      case None => None
    }
  }

  private def readDB = {
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
    val newID = usersDB.length + 1
    val newUser = RegisteredUser(
      id = newID,
      username = user.username,
      password = hashPassword(user).password
    )
    val file = FileOutputStream(dbPath)
    val json = Json.toJson(usersDB.::(newUser))
    file.write(Json.prettyPrint(json).getBytes())
    file.flush()
    file.close()
    val userDirectory = Paths.get(s"$imagePath/${newUser.username}")
    if (!Files.exists(userDirectory))
      Files.createDirectories(userDirectory)
  }

  private def getUser(user: User, db: List[RegisteredUser]): Option[RegisteredUser] = {
    db.find(_.username == user.username)
  }

  def userExists(username: String): Boolean = {
    val usersDB = readDB
    getUserByUsername(username, usersDB) match {
      case Some(_) => true
      case _ => false
    }
  }

  private def getUserByUsername(username: String, usersDB: List[RegisteredUser]): Option[RegisteredUser] = {
    usersDB.find(_.username == username)
  }
  
  def getUserID(request: RequestHeader): Option[Int] = {
    val usersDB = readDB
    val username = getUsername(request)
    username match {
      case Some(username) => getUserByUsername(username, usersDB) match {
        case Some(user) => Some(user.id)
        case _ => None
      }
      case _ => None
    }
  }

  def loginUser(user: User): String = {
    val usersDB = readDB
    val formattedUser = user.copy(username = user.username.toLowerCase())
    val someUser = getUser(formattedUser, usersDB)
    someUser match {
      case Some(registeredUser) =>
        if (checkPassword(formattedUser, registeredUser.password)) {
          val token = generateToken(registeredUser.username)
          token
        } else {
          throw new Exception("Wrong password")
        }
      case _ => throw new Exception("User not found")
    }
  }

  def registerUser(user: User): Unit = {
    val usersDB = readDB
    val someUser = getUser(user, usersDB)
    someUser match {
      case Some(_) => throw new Exception("User already exists")
      case None => saveUser(user, usersDB)
    }
  }
  
  def changePassword(username: String, password: String): Unit = {
    val usersDB = readDB
    val oldUser = getUserByUsername(username, usersDB).get
    val newUser = oldUser.copy(password = BCrypt.hashpw(password, BCrypt.gensalt()))
    val newDB = usersDB.filterNot(_.username == username).::(newUser)
    val file = FileOutputStream(dbPath)
    val json = Json.toJson(newDB)
    file.write(Json.prettyPrint(json).getBytes())
    file.flush()
    file.close()
  }
}
