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
  val scn = scenario("Login").
    exec(http("login")
      .post(s"users/login")
      .body(StringBody(s"""{"email": "$email", "password": "$password"}""")).asJson
         //Validar status 200 del servicio
      .check(status.is(200)
    
         
      .and(jsonPath("$.token").saveAs("authToken")),
             jsonPath("$.message").optional.saveAs("errorMessage")
      )
      .exitHereIfFailed
    )
  .exec { session =>
    // Comprobamos si vino el token
    if (session.contains("authToken")) {
      println(s"✅ Login exitoso para ${session("email").as[String]}")
    } else {
      val email = session("email").asOption[String].getOrElse("usuario desconocido")
      val errorMsg = session("errorMessage").asOption[String].getOrElse("Sin mensaje de error")
      println(s"❌ Login fallido para $email. Mensaje recibido: $errorMsg")
      
      // Aquí simulas que el usuario permanece en la página de login
      session.markAsFailed // Gatling lo marca como un error en el reporte
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
