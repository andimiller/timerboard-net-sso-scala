import cats.effect.IO
import doobie.util.transactor.Transactor
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, MustMatchers}
import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import cats.effect.IO
import cats._
import cats.data._
import cats.implicits._
import java.time.Instant
import java.time.temporal.{ChronoUnit, TemporalUnit}

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.{ExposedPort, Ports}
import com.github.dockerjava.core.DockerClientBuilder

class DBSpec extends FlatSpec with MustMatchers with BeforeAndAfterAll with BeforeAndAfter {

  var docker: DockerClient = null
  var postgresContainer: String = null

  override def beforeAll(): Unit = {
    docker = DockerClientBuilder.getInstance("tcp://localhost:2375").build()
    val ports = new Ports()
    val tcp5432 = ExposedPort.tcp(5432)
    ports.bind(tcp5432, Ports.Binding.bindPort(32770))
    val container = docker.createContainerCmd("postgres:latest")
      .withEnv("POSTGRES_PASSWORD=foobar")
      .withExposedPorts(tcp5432)
      .withPortBindings(ports)
      .exec()
    postgresContainer = container.getId
    docker.startContainerCmd(container.getId).exec()
    Thread.sleep(3000)
  }

  override def afterAll(): Unit = {
    docker.stopContainerCmd(postgresContainer).exec()
  }

  lazy val xa = Transactor.fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql://localhost:32770/postgres", "postgres", "foobar")

  before {
    {
      for {
        _ <- DB.cleanup()
        _ <- DB.provision()
      } yield ()
    }.transact(xa).unsafeRunSync()
  }

  "DB.Groups" should "store a group and query it back out" in {
    {
      val group = Models.Group(None, "testgroup", List(1, 2), List(2))
      for {
        inserted <- DB.Groups.insert(group)
        byname   <- DB.Groups.getByName("testgroup")
        byid     <- DB.Groups.getById(1)
      } yield {
        inserted must equal(1)
        byname   must equal(Some(group.copy(id = Some(1))))
        byid     must equal(Some(group.copy(id = Some(1))))
      }
    }.transact(xa).unsafeRunSync
  }

  "DB.Groups" should "list groups" in {
   {
     val groups = (1 to 10).map { i =>
       Models.Group(None, s"group$i", List(i), List(i))
     }.toList
     for {
       _ <- DB.Groups.insert(groups(0))
       _ <- DB.Groups.insert(groups(1))
       _ <- DB.Groups.insert(groups(2))
       _ <- DB.Groups.insert(groups(3))
       _ <- DB.Groups.insert(groups(4))
       _ <- DB.Groups.insert(groups(5))
       _ <- DB.Groups.insert(groups(6))
       _ <- DB.Groups.insert(groups(7))
       _ <- DB.Groups.insert(groups(8))
       _ <- DB.Groups.insert(groups(9))
       list <- DB.Groups.list()
     } yield {
       list must equal(groups.zipWithIndex.map{case (g, i) => g.copy(id=Some(i+1))})
     }
   }.transact(xa).unsafeRunSync
  }

}
