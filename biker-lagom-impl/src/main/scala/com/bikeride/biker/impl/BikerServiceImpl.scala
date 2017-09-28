package com.bikeride.biker.impl

import java.util.UUID

import akka.NotUsed
import akka.stream.Materializer
import com.bikeride.biker.api
import com.bikeride.biker.api.BikerService
import com.bikeride.utils.security.ServerSecurity.authenticated
import com.bikeride.utils.security._
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}
import com.lightbend.lagom.scaladsl.persistence.{PersistentEntityRegistry, ReadSide}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.server.ServerServiceCall

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class BikerServiceImpl (bikerService: BikerService,
                        persistentEntityRegistry: PersistentEntityRegistry,
                        session: CassandraSession,
                        readside: ReadSide)
                        (implicit ec: ExecutionContext, mat: Materializer) extends BikerService{

  private def refFor(id: UUID) = persistentEntityRegistry.refFor[BikerEntity](id.toString)

  override def createBiker() = ServiceCall { req =>
    val bikerId = UUID.randomUUID()
    refFor(bikerId).ask(CreateBiker(BikerState(bikerId,
                                               req.bikerFields.name,
                                               req.bikerFields.avatarb64,
                                               req.bikerFields.bloodType,
                                               req.bikerFields.mobile,
                                               req.bikerFields.email,
                                               req.bikerFields.active))).map { _ =>
      val tokenContent = TokenContent(req.client.clientID,bikerId,req.bikerFields.name,false)
      //TODO discover how the refresh mechanism works
      val token = JwtTokenUtil.generateAuthTokenOnly(tokenContent)
      api.BikerToken(api.BikerID(bikerId),token)
    }
  }

  //override def changeBikerName(bikerId: UUID) = authenticated( userId => ServerServiceCall { req =>
  override def changeBikerName(bikerId: UUID) = ServiceCall { req =>
    if (req.name.isEmpty) throw BadRequest("Biker name cannot be empty!")
    else{
      refFor(bikerId).ask(ChangeBikerName(BikerChange(bikerId,req.name,req.avatarb64,req.bloodType,req.mobile,req.email))).map { _ =>
        api.BikerID(bikerId)
      }
    }
  }//)

  //override def changeBikerAvatarB64(bikerId: UUID) = authenticated( userId => ServerServiceCall { req =>
  override def changeBikerAvatarB64(bikerId: UUID) = ServiceCall { req =>
    if (req.avatarb64.isEmpty)throw BadRequest("Biker avatarB64 cannot be empty!")
    else{
      refFor(bikerId).ask(ChangeBikerAvatarB64(BikerChange(bikerId,req.name,req.avatarb64,req.bloodType,req.mobile,req.email))).map { _ =>
        api.BikerID(bikerId)
      }
    }
  }//)

  //override def changeBikerBloodType(bikerId: UUID) = authenticated( userId => ServerServiceCall { req =>
  override def changeBikerBloodType(bikerId: UUID) = ServiceCall { req =>
    if (req.bloodType.isEmpty) throw BadRequest("Biker blood type cannot be empty!")
    else{
      refFor(bikerId).ask(ChangeBikerBloodType(BikerChange(bikerId,req.name,req.avatarb64,req.bloodType,req.mobile,req.email))).map { _ =>
        api.BikerID(bikerId)
      }
    }
  }//)

  //override def changeBikerMobile(bikerId: UUID) = authenticated( userId => ServerServiceCall { req =>
  override def changeBikerMobile(bikerId: UUID) = ServiceCall { req =>
    if (req.mobile.isEmpty) throw BadRequest("Biker mobile cannot be empty!")
    else{
      refFor(bikerId).ask(ChangeBikerMobile(BikerChange(bikerId,req.name,req.avatarb64,req.bloodType,req.mobile,req.email))).map { _ =>
        api.BikerID(bikerId)
      }
    }
  }//)

  //override def changeBikerEmail(bikerId: UUID) = authenticated( userId => ServerServiceCall { req =>
  override def changeBikerEmail(bikerId: UUID) = ServiceCall { req =>
    if (req.email.isEmpty) throw BadRequest("Biker email cannot be empty!")
    else{
      refFor(bikerId).ask(ChangeBikerEmail(BikerChange(bikerId,req.name,req.avatarb64,req.bloodType,req.mobile,req.email))).map { _ =>
        api.BikerID(bikerId)
      }
    }
  }//)

  //override def activateBiker(bikerId: UUID) = authenticated( userId => ServerServiceCall { req =>
  override def activateBiker(bikerId: UUID) = ServiceCall { req =>
    refFor(bikerId).ask(ActivateBiker(bikerId)).map { _ =>
      api.BikerID(bikerId)
    }
  }//)

  override def deactivateBiker(bikerId: UUID) = ServiceCall { req =>
  //override def deactivateBiker(bikerId: UUID) = authenticated( userId => ServerServiceCall { req =>
    refFor(bikerId).ask(DeactivateBiker(bikerId)).map { _ =>
      api.BikerID(bikerId)
    }
  }//)

  //override def getBikerIsActive(bikerId: UUID) = authenticated( userId => ServerServiceCall { _ =>
  override def getBikerIsActive(bikerId: UUID) = ServiceCall { _ =>
    refFor(bikerId).ask(GetBiker).map {
      case Some(biker) =>
        api.BikerIsActive(bikerId,biker.active)
      case None =>
        throw NotFound(s"Biker with id $bikerId")
    }
  }//)

  //override def getBikerAvatarB64(bikerId: UUID) = authenticated( userId => ServerServiceCall { _ =>
  override def getBikerAvatarB64(bikerId: UUID) = ServiceCall { _ =>
    refFor(bikerId).ask(GetBiker).map {
      case Some(biker) =>
        api.BikerAvatarB64(bikerId,biker.avatarb64)
      case None =>
        throw NotFound(s"Biker with id $bikerId")
    }
  }//)

  //override def getBiker(bikerId: UUID) = authenticated( userId => ServerServiceCall { _ =>
  override def getBiker(bikerId: UUID) = ServiceCall { _ =>
    refFor(bikerId).ask(GetBiker).map {
      case Some(biker) =>
        api.Biker(api.BikerID(bikerId),api.BikerFields(biker.name,biker.avatarb64,biker.bloodType,biker.mobile,biker.email,biker.active))
      case None =>
        throw NotFound(s"Biker with id $bikerId")
    }
  }//)

  private val DefaultPageSize = 10
  //override def getBikers( pageNo: Option[Int], pageSize: Option[Int]) = authenticated( userId => ServerServiceCall { req =>
  //override def getBikers( pageNo: Option[Int], pageSize: Option[Int]) = authenticated( userId => ServerServiceCall[NotUsed, Seq[api.Biker]] { req =>
  override def getBikers( pageNo: Option[Int], pageSize: Option[Int]) = ServiceCall[NotUsed, Seq[api.Biker]] { req =>
    //TODO implement the pages in the query
    println(s"getBikers(${pageNo.getOrElse(0)}, ${pageSize.getOrElse(DefaultPageSize)})   ##############")
    //val offset = pageNo * pageSize
    //.drop(offset)
    session.select("SELECT id,name,avatarb64,bloodType,mobile,email,active FROM bikers LIMIT ?",Integer.valueOf(pageSize.getOrElse(DefaultPageSize))).map { row =>
      api.Biker(
        api.BikerID(row.getUUID("id")),
        api.BikerFields(
          row.getString("name"),
          Some(row.getString("avatarb64")),
          Some(row.getString("bloodtype")),
          Some(row.getString("mobile")),
          Some(row.getString("email")),
          row.getBool("active")
        )
      )
    }.runFold(Seq.empty[api.Biker])((acc, e) => acc :+ e)
  }//)

  override def getBikerByEmail = ServiceCall { req =>
    //TODO: get this from the materialized view
    //session.select("SELECT id,name FROM biker_contacts WHERE email=?",req.email).map {row =>
    //  println(row.getString("id")+":"+row.getString("name"))
    //}

    //session.select("SELECT id,name,avatarb64,bloodType,mobile,email,active FROM bikers WHERE email=?",req.email).map { row =>
    //  api.Biker(
    //    api.BikerID(row.getUUID("id")),
    //    api.BikerFields(
    //      row.getString("name"),
    //      Some(row.getString("avatarb64")),
    //      Some(row.getString("bloodtype")),
    //      Some(row.getString("mobile")),
    //      Some(row.getString("email")),
    //      row.getBool("active")
    //    )
    //  )
    //}

    Future(api.BikerByEmailResponse(
      Some(api.Biker(
        api.BikerID(UUID.randomUUID()),
        api.BikerFields(
          "Name",
          Some("avatarb64"),
          Some("bloodtype"),
          Some("mobile"),
          Some("email"),true
        )
      ))
    ))
  }//)
}