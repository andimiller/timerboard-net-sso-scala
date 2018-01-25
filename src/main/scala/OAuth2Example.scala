import cats.effect.IO
import fs2.StreamApp
import org.http4s.{HttpService, Uri}
import org.http4s.client.blaze.{BlazeClient, Http1Client, PooledHttp1Client}
import org.http4s.server.blaze.BlazeBuilder
import io.circe.generic.auto._
import io.circe._
import io.circe.syntax._
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._

object OAuth2Example extends StreamApp[IO] with Http4sDsl[IO] {
  case class VerifyPayload(
      CharacterID: Long,
      CharacterName: String,
      ExpiresOn: String,
      Scopes: String,
      TokenType: String,
      CharacterOwnerHash: String
  )

  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    val oauth2 = new OAuth2[IO, VerifyPayload](
      Uri.uri("https://login.eveonline.com/oauth/authorize"),
      Uri.uri("https://login.eveonline.com/oauth/token"),
      "clientid",
      "clientsecret",
      Uri.uri("http://localhost:8080/callback"),
      Uri.uri("https://login.eveonline.com/oauth/verify")
    )(
      Http1Client.apply[IO]().unsafeRunSync()
    )(f => Ok(f.asJson))

    val main = HttpService[IO] {
      case req @ GET -> Root =>
        TemporaryRedirect(oauth2.redirect(List.empty[String], "examplestate"))
    }

    org.apache.log4j.BasicConfigurator.configure()

    BlazeBuilder[IO]
      .bindHttp(8080, "localhost")
      .mountService(main, "/")
      .mountService(oauth2.callback, "/callback")
      .serve

  }
}
