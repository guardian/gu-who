package lib

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink}

import scala.concurrent.Future

// TODO - move to play-git-hub
package object gitgithub {
  implicit class RichSource[T](s: akka.stream.scaladsl.Source[Seq[T], NotUsed]) {
    def all()(implicit mat: Materializer): Future[Seq[T]] = s.toMat(Sink.reduce[Seq[T]](_ ++ _))(Keep.right).run()
  }
}
