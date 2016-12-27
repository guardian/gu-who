FROM ubuntu:14.04
FROM java:8

ENV SCALA_VERSION 2.12.1 
ENV SBT_VERSION 0.13.13
ENV THREADS=4
ENV APPLICATION_SECRET=guwho
# Install Basic stuff
RUN apt-get update && apt-get install -y gcc g++ libc6-dev build-essential libffi-dev libssl-dev git curl

# Install Scala
RUN \
  curl -fsL http://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
  echo >> /root/.bashrc && \
  echo 'export PATH=~/scala-$SCALA_VERSION/bin:$PATH' >> /root/.bashrc

# Install sbt
RUN \
  curl -L -o sbt-$SBT_VERSION.deb http://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  sbt sbtVersion

WORKDIR /src

# Bundle app source
ADD . /src

# Stage the application
RUN sbt stage
