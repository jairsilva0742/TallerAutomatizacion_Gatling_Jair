package Demo

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import Demo.Data._

class LoginTest extends Simulation{
  val feeder = csv("contactos.csv").circular
  // 1 Http Conf
  val httpConf = http.baseUrl(url)
    .acceptHeader("application/json")
    //Verificar de forma general para todas las solicitudes
    .check(status.is(200))

  // 2 Definicion de escenario
  val scn = scenario("Login")
  .feed(loginFeeder)
  .exec { session =>
    val email = session("email").as[String]
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".r

    email match {
      case emailRegex() =>
        session // Email válido, continúa
      case _ =>
        println(s"⚠️ Email inválido detectado: $email — se detiene el escenario")
        session.markAsFailed // Se puede usar exitHere o marcar fallo
    }
  }
  .exitHereIfFailed // Detiene el usuario si el email era inválido
  .exec(http("login")
      .post(s"users/login")
      .body(StringBody(s"""{"email": "$email", "password": "$password"}""")).asJson
         //Validar status 200 del servicio
      .check(status.is(200))
      .check(jsonPath("$.token").saveAs("authToken"))
    )
  .exec { session =>
    if (session.isFailed) {
      println("Incorrect email or password")
    } 
    session
  }

  .feed(feeder)
  .exec(
      http("Create Contact")
        .post(s"contacts")
        .header("Authorization", "Bearer ${authToken}")
        .body(StringBody(
        """{
          "firstName": "${firstName}",
          "lastName": "${lastName}",
          "birthdate": "${birthdate}",
          "email": "${email}",
          "phone": "${phone}",
          "street1": "${street1}",
          "street2": "${street2}",
          "city": "${city}",
          "stateProvince": "${stateProvince}",
          "postalCode": "${postalCode}",
          "country": "${country}"
        }"""
      )).asJson
        .check(status.is(201))
    )

  // 3 Load Scenario
  setUp(
    scn.inject(rampUsersPerSec(2).to(8).during(30))
  ).protocols(httpConf);
}
