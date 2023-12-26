package services

import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtJson}
import com.typesafe.config.ConfigFactory
import models.{RegisteredUser, User}
import org.mindrot.jbcrypt.BCrypt
import play.api.libs.json.{Json, OFormat, Writes}

import java.io.{File, FileInputStream, FileOutputStream}
import java.time.Clock

object AuthService {
  implicit val clock: Clock = Clock.systemUTC
  private val conf = ConfigFactory.load("./conf/application.conf")
  private val secretKey = conf.getString("application.secret_key")
  private val algo = JwtAlgorithm.HS256
  private val dbPath = "./resources/userdb.json"
  implicit val registeredUserFormat: OFormat[RegisteredUser] = Json.format[RegisteredUser]

  def generateToken(userID: Int): String = {
    val claim = JwtClaim().issuedNow.expiresIn(3600).+("user_id", userID)
    JwtJson.encode(claim, secretKey, algo)
  }

  def refreshToken(token: String): String = {
    val claim = JwtJson.decode(token, secretKey, Seq(algo)).get
    val newClaim = claim.issuedNow.expiresIn(3600)
    JwtJson.encode(newClaim, secretKey, algo)
  }

  def verifyToken(token: String): Boolean = {
    JwtJson.isValid(token, secretKey, Seq(algo))
  }

  private def readDB = {
    val file = FileInputStream(dbPath)
    val json = Json.parse(file).as[List[RegisteredUser]]
    file.close()
    json
  }

  def hashPassword(user: User): User = {
    user.copy(password = BCrypt.hashpw(user.password, BCrypt.gensalt()))
  }

  def checkPassword(user: User, password: String): Boolean = {
    BCrypt.checkpw(password, user.password)
  }

  def saveUser(user: User)(using formatter: OFormat[RegisteredUser]): Unit = {
    val newID = readDB.length + 1
    val newUser = RegisteredUser(
      id = newID,
      username = user.username,
      password = hashPassword(user).password
    )
    val file = FileOutputStream(dbPath)
    val json = Json.toJson(readDB :+ newUser)
    file.write(json.toString().getBytes)
    file.flush()
    file.close()
  }

  def getUser(user: User): Option[RegisteredUser] = {
    readDB.find(_.username == user.username)
  }

  def loginUser(user: User) = {
    val someUser = getUser(user)
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

  def registerUser(user: User) = {
    val someUser = getUser(user)
    someUser match {
      case Some(_) => throw new Exception("User already exists")
      case None => saveUser(user)
    }
  }
}
