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

object DB extends App {

  def provision(): ConnectionIO[Int] = sql"""create table "groups" (
                                              id SERIAL PRIMARY KEY,
                                              name VARCHAR(24) UNIQUE NOT NULL,
                                              admins Int4[] NOT NULL ,
                                              members Int4[] NOT NULL
                                            );

                                            create table "timers" (
                                              id SERIAL PRIMARY KEY,
                                              "user" Int4 NOT NULL,
                                              system VARCHAR(12) NOT NULL,
                                              planet VARCHAR(6) NOT NULL,
                                              moon Int4,
                                              owner VARCHAR(64) NOT NULL,
                                              time TIMESTAMP NOT NULL,
                                              visibility Int4[] NOT NULL,
                                              type VARCHAR(32) NOT NULL
                                            );""".update.run

  def cleanup(): ConnectionIO[Int] = sql"""drop table if exists "groups";
                                           drop table if exists "timers";
                                           """.update.run


  object Groups {
    def insert(g: Models.Group): ConnectionIO[Int] =
      sql"insert into groups (name, admins, members) values (${g.name}, ${g.admins}, ${g.members})".update.run
    def list(): ConnectionIO[List[Models.Group]] =
      sql"select id, name, admins, members from groups".query[Models.Group].list
    def getById(id: Int): ConnectionIO[Option[Models.Group]] =
      sql"select id, name, admins, members from groups where id = $id".query[Models.Group].option
    def getByName(name: String): ConnectionIO[Option[Models.Group]] =
      sql"select id, name, admins, members from groups where name = $name".query[Models.Group].option
    def clear(): ConnectionIO[Int] =
      sql"delete from groups where 1=1".update.run
  }

  object Timers {
    def insert(t: Models.Timer): ConnectionIO[Int]
    = sql"""insert into timers ("user", system, planet, moon, owner, time, visibility, type) VALUES
           (${t.user}, ${t.system}, ${t.planet}, ${t.moon}, ${t.owner}, ${t.time}, ${t.visibility}, ${t.`type`})
      """.update.run
    def listVisible(filter: List[Int]): ConnectionIO[List[Models.Timer]] =
      sql"""SELECT id, "user", system, planet, moon, owner, time, visibility, type FROM timers
            where visibility && $filter""".query[Models.Timer].list
    def clear(): ConnectionIO[Int] =
      sql"delete from timers where 1=1".update.run

  }

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql://localhost:32769/postgres", "postgres", "foobar"
  )

  val g1 = Models.Group(None, "testgroup2", List(1), List(2))
  val program = for {
    _         <- DB.Groups.clear()
    count     <- DB.Groups.insert(g1)
    listed    <- DB.Groups.list()
    one       <- DB.Groups.getById(1)
    two       <- DB.Groups.getById(2)
    testgroup <- DB.Groups.getByName("testgroup")
  } yield (listed, one, two, testgroup)

  val t1 = Models.Timer(None, 90758388, "6VDT-H", "1", Some(4), "Test Alliance Please Ignore", Instant.now(), List(90758388), "IHub Shield")
  val t2 = t1.copy(time = Instant.now().minus(2, ChronoUnit.DAYS))
  val program2 = for {
    deleted   <- DB.Timers.clear()
    inserted  <- DB.Timers.insert(t1)
    a         <- DB.Timers.listVisible(List(90758388))
    b         <- DB.Timers.listVisible(List.empty[Int])
    inserted2 <- DB.Timers.insert(t2)
    c         <- DB.Timers.listVisible(List(90758388))
    d         <- DB.Timers.listVisible(List.empty[Int])
  } yield {
    val text =
      s"""cleared $deleted rows
         |inserted $inserted timers
         |querying as myself: $a
         |querying as nobody: $b
         |inserted $inserted2 extra timers
         |querying as myself: $c
         |querying as nobody: $d
       """.stripMargin
    println(text)
  }
  val result = program2.transact(xa).unsafeRunSync
  println(result)


}
