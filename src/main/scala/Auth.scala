import java.util.UUID

import cats._
import cats.data.OptionT
import cats.effect.{IO, Sync}
import fs2.StreamApp
import org.http4s.HttpService
import org.http4s.dsl.io._
import org.http4s.server.blaze.BlazeBuilder
import tsec.authentication._
import tsec.authorization._
import tsec.cipher.symmetric.imports._
import tsec.common.SecureRandomId
import tsec.jws.mac.JWTMac

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._

import Models.User


object Auth extends StreamApp[IO] {

  def trieMapBackingStore[F[_], I, V](getId: V => I)(implicit F: Sync[F]) = new BackingStore[F, I, V] {
    private val storageMap = TrieMap.empty[I, V]

    def put(elem: V): F[V] = {
      val map = storageMap.put(getId(elem), elem)
      if (map.isEmpty)
        F.pure(elem)
      else
        F.raiseError(new IllegalArgumentException)
    }

    def get(id: I): OptionT[F, V] =
      OptionT.fromOption[F](storageMap.get(id))

    def update(v: V): F[V] = {
      storageMap.update(getId(v), v)
      F.pure(v)
    }

    def delete(id: I): F[Unit] =
      storageMap.remove(id) match {
        case Some(_) => F.unit
        case None    => F.raiseError(new IllegalArgumentException)
      }
  }




  // usage
  val bearerTokenStore = trieMapBackingStore[IO, SecureRandomId, TSecBearerToken[Long]](s => SecureRandomId.coerce(s.id))
  val userStore = trieMapBackingStore[IO, Long, User](_.id)

  val settings = TSecTokenSettings(
    expiryDuration = 1.hour,
    maxIdle = None
  )

  val bearerTokenAuth = BearerTokenAuthenticator(bearerTokenStore, userStore, settings)

  val Authed = SecuredRequestHandler(bearerTokenAuth)

  val authedService: HttpService[IO] = Authed {
    case request @ GET -> Root asAuthed user =>
      Ok(user.id.toString)
  }

  val loginService: HttpService[IO] = HttpService {
    case req @ GET -> Root =>
      for {
        token <- Authed.authenticator.create(1).value
        _     <- userStore.put(User(1, "Lucia Denniard", 2, 3))
        resp  <- Ok(token.get.id.toString)
      } yield resp
  }

  def stream(args: List[String], requestShutdown: IO[Unit]) = BlazeBuilder[IO]
    .bindHttp(8080, "localhost")
    .mountService(loginService, "/login")
    .mountService(authedService, "/authed")
    .serve
}
