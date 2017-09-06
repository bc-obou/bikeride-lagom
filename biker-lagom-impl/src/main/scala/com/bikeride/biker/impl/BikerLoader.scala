package com.bikeride.biker.impl

import com.bikeride.biker.api.BikerService
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.softwaremill.macwire._

class BikerLoader extends LagomApplicationLoader{

  override def load(context: LagomApplicationContext): LagomApplication =
    new BikerApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new BikerApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[BikerService])
}

abstract class BikerApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  override lazy val lagomServer = serverFor[BikerService](wire[BikerServiceImpl])
  override lazy val jsonSerializerRegistry = BikerSerializerRegistry

  lazy val bikerService = serviceClient.implement[BikerService]

  persistentEntityRegistry.register(wire[BikerEntity])
  readSide.register(wire[BikerEventProcessor])

}