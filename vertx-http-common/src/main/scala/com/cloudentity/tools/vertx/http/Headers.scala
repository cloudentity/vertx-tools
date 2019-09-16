package com.cloudentity.tools.vertx.http

import scala.collection.immutable.TreeMap

object Headers {
  implicit val ordering = new Ordering[String] {
    override def compare(x: String, y: String): Int = x.compareToIgnoreCase(y)
  }

  def apply(hs: Map[String, List[String]]): Headers =
    new Headers(TreeMap(hs.toList:_*))

  def of(hs: Map[String, String]): Headers =
    new Headers(TreeMap(hs.mapValues(List(_)).toList:_*))

  def of(hs: (String, String)*): Headers =
    apply(hs.groupBy(_._1).mapValues(_.map(_._2).toList).toList:_*)

  def apply(hs: (String, List[String])*): Headers =
    new Headers(TreeMap[String, List[String]](hs:_*))
}

case class Headers private (private val headers: TreeMap[String, List[String]]) {
  def toMap: Map[String, List[String]] = headers

  def get(name: String): Option[String] =
    headers.get(name).flatMap(_.headOption)

  def getValues(name: String): Option[List[String]] =
    headers.get(name)

  def set(name: String, value: String): Headers =
    this.copy(headers = headers.updated(name, List(value)))

  def setHeaders(setHs: Map[String, String]): Headers =
    setHs.foldLeft(this) { case (hs, (key, value)) => hs.set(key, value) }

  def setValues(name: String, values: List[String]): Headers =
    this.copy(headers = headers.updated(name, values))

  def remove(name: String): Headers =
    this.copy(headers = headers - name)

  def remove(name: String, value: String): Headers =
    headers.get(name) match {
      case Some(values) =>
        val filtered = values.filter(value != _)
        if (filtered.nonEmpty) this.setValues(name, filtered) else remove(name)
      case None => this
    }

  def add(name: String, value: String): Headers =
    headers.get(name) match {
      case Some(oldValues) => this.setValues(name, oldValues ::: List(value))
      case None            => this.set(name, value)
    }

  def addValues(name: String, values: List[String]): Headers =
    headers.get(name) match {
      case Some(oldValues) => this.setValues(name, oldValues ::: values)
      case None            => this.setValues(name, values)
    }

  def addHeaders(addHs: Map[String, String]): Headers =
    addHs.foldLeft(this) { case (hs, (key, value)) => hs.add(key, value) }

  def addMultiHeaders(values: Map[String, List[String]]): Headers =
    values.foldLeft(this) { case (hs, (key, values)) => hs.addValues(key, values) }

  def contains(name: String): Boolean =
    get(name).isDefined

  def contains(name: String, value: String): Boolean =
    getValues(name).getOrElse(Nil).contains(value)

  def exists(p: ((String, List[String])) => Boolean): Boolean =
    headers.exists(p)
}