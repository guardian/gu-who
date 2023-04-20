/*
 * Copyright 2014 The Guardian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lib

import scala.jdk.FutureConverters._

import com.gu.who.logging.Logging
import com.madgag.scalagithub.GitHub
import com.madgag.scalagithub.model.Repo

import java.net.URI
import java.net.http.HttpClient.Redirect
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration.ofSeconds
import java.util.stream
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

object PeopleRepo extends Logging {

  val client: HttpClient = HttpClient.newBuilder.version(HttpClient.Version.HTTP_2).followRedirects(Redirect.NORMAL)
    .connectTimeout(ofSeconds(20)).build

  def request(uri: URI) = HttpRequest.newBuilder(uri).GET().build()

  val UsernameRegex: Regex = """^([\p{Alnum}-]{2,}+)""".r

  def getSponsoredUserLogins(repo: Repo)(implicit ec: ExecutionContext, g: GitHub): Future[Set[String]] = {

    for {
      content <- repo.contentsFile.get("users.txt")
      usersContentResponse: HttpResponse[String] <-
        client.sendAsync(request(URI.create(content.download_url)), BodyHandlers.ofString).asScala
    } yield {
      // if usersContentResponse.statusCode == 200
      val sponsoredUserNames: Set[String] =
        usersContentResponse.body().linesIterator.collect { case UsernameRegex(username) => username }.toSet

      logger.info(s"Found ${sponsoredUserNames.size} sponsored usernames")
      sponsoredUserNames
    }
  }

}
