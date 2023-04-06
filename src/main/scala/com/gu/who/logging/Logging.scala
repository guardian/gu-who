package com.gu.who.logging

import net.logstash.logback.marker.LogstashMarker
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

trait Logging {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  implicit def mapToContext(c: Map[String, _]): LogstashMarker = appendEntries(c.asJava)

}
