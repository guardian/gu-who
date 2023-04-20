package com.gu.who.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import com.amazonaws.services.lambda.runtime.LambdaRuntimeInternal
import net.logstash.logback.layout.LogstashLayout
import com.gu.who.BuildInfo

object UniqueIdForVM {
  val id = java.util.UUID.randomUUID.toString
}

class LogstashLayoutWithBuildInfo extends LogstashLayout {
  LambdaRuntimeInternal.setUseLog4jAppender(true)

  val contextTags: Map[String, String] = Map(
    "buildNumber" -> BuildInfo.buildNumber,
    "gitCommitId" -> BuildInfo.gitCommitId,
    "uniqueIdForVM" -> UniqueIdForVM.id // 'AWSRequestId' seems only intermittently available, this is an alternative
  )

  setCustomFields(upickle.default.write(contextTags))

  override def doLayout(event: ILoggingEvent): String = super.doLayout(event)+"\n"
}
