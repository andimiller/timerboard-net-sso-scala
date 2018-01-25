import cats._
import cats.syntax._
import cats.implicits._
import cats.effect._
import io.circe.Decoder
import org.http4s.Status.Redirection
import org.http4s.Uri
import org.http4s.client
import org.http4s.client.Client
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization

class OAuth2[E[_]: Effect: Monad, LoginData: Decoder](
    authorizationURL: Uri,
    tokenURL: Uri,
    clientId: String,
    clientSecret: String,
    callbackURL: Uri,
    userdataURL: Uri
)(
    client: Client[E]
)(
    callbackFn: OAuth2.CallbackPayload[LoginData] => E[Response[E]]
) {
  import OAuth2.TokenPayload

  def redirect(scopes: List[String], state: String) =
    authorizationURL
      .withQueryParam("response_type", "code")
      .withQueryParam("redirect_uri", callbackURL.toString())
      .withQueryParam("client_id", clientId)
      .withQueryParam("scope", scopes.mkString(" "))
      .withQueryParam("state", state)

  private val dsl = Http4sDsl[E]
  import dsl._

  def callback = HttpService[E] {
    case req @ GET -> Root =>
      req.decode[UrlForm] { f =>
        {
          for {
            code  <- f.getFirst("code")
            state <- f.getFirst("state")
          } yield {
            for {
              token <- client.expect[TokenPayload](
                Request[E](
                  method  = Method.POST,
                  uri     = tokenURL,
                  headers = Headers(Authorization(BasicCredentials(clientId, clientSecret)))
                ).withBody(UrlForm("grant_type" -> "authorization_code", "code" -> code))
              )(jsonOf[E, TokenPayload])
              extradata <- client.expect[LoginData](
                Request[E](
                  method  = Method.GET,
                  uri     = userdataURL,
                  headers = Headers(Authorization(Credentials.Token(AuthScheme.Bearer, token.access_token)))
                )
              )(jsonOf[E, LoginData])
              resp <- callbackFn(OAuth2.CallbackPayload(token, state, extradata))
            } yield resp
          }
        }.getOrElse(InternalServerError())
      }
  }
}

object OAuth2 {
  case class CallbackPayload[T](
      token: TokenPayload,
      state: String,
      logindata: T
  )

  case class TokenPayload(
      access_token: String,
      token_type: String,
      expires_in: Int,
      refresh_token: Option[String]
  )
  object TokenPayload {
    import _root_.io.circe.generic.auto._
    implicit val tokenPayloadDecoder: Decoder[TokenPayload] = implicitly[Decoder[TokenPayload]]
  }
}
