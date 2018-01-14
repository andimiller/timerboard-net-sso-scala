import java.time.Instant

object Models {

  case class Group(
      id: Option[Int],
      name: String,
      admins: List[Int],
      members: List[Int]
  )

  case class Timer(
      id: Option[Int],
      user: Int,
      system: String,
      planet: String,
      moon: Option[Int],
      owner: String,
      time: Instant,
      visibility: List[Int],
      `type`: String
  )
}
