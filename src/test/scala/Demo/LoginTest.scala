package Demo

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import Demo.Data._


class LoginTest extends Simulation{
  //Acá se define la base de datos para obtener los contactos y los login
  val feeder = csv("contactos.csv").circular
  // 1 Http Conf
  
  
  val httpConf = http.baseUrl(url)
    .acceptHeader("application/json")
    //Verificar de forma general para todas las solicitudes
    .check(status.is(200))

  // 2 Definicion de escenario
  val scn = scenario("Login")
    .exec(http("login")
      .post(s"users/login")
      .body(StringBody(s"""{"email": "$email", "password": "$password"}""")).asJson
         //Validar status 200 del servicio
      .check(status.is(200))
      .check(jsonPath("$.token").saveAs("authToken"))
    )

  .exec { session =>
    //if (!(email.contains("@") || email.contains("."))) {
      //println("Email inválido")
      //session.markAsFailed
    //}
    if (session.isFailed) {
      //Si el inicio de sesion falla se envía mensaje solicitado
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
        .check(status.in(201))
    )
    setUp(
    scn.inject(
      atOnceUsers(5),         // 5 usuarios inmediatamente
      rampUsers(5).during(5)  // 5 usuarios en 5 segundos
    )
  ).protocols(httpConf)

  
}
