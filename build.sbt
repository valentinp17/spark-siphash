/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

name := "spark-siphash"

version := "1.0.0"
scalaVersion := "2.12.18"
crossScalaVersions := Seq("2.12.18", "2.13.8")

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))

val sparkVersion = "3.3.4"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
  "org.apache.spark" %% "spark-core" % sparkVersion % Test classifier "tests",
  "org.apache.spark" %% "spark-sql" % sparkVersion % Provided,
  "org.apache.spark" %% "spark-sql" % sparkVersion % Test classifier "tests",
  "org.apache.spark" %% "spark-catalyst" % sparkVersion % Provided,
  "org.apache.spark" %% "spark-catalyst" % sparkVersion % Test classifier "tests",

  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "org.scalacheck" %% "scalacheck" % "1.18.0" % Test,
  "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0" % Test
)

organization := "com.valentinp"
description := "Java Siphash as a Spark Expression"
developers := List(Developer(
  "valentinp", "Valentin Poletaev", "pvv377@gmail.com",
  url("https://github.com/valentinp17")
))
homepage := Some(url("https://github.com/valentinp17/spark-siphash"))

publishMavenStyle := true
isSnapshot := true
