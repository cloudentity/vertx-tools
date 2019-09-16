package com.cloudentity.tools.vertx.server.api.routes

import com.cloudentity.tools.vertx.scala.Operation
import com.cloudentity.tools.api.errors.ApiError
import com.cloudentity.tools.vertx.scala.bus.ScalaServiceVerticle
import com.cloudentity.tools.vertx.scala.VertxExecutionContext
import io.vertx.core.{Future => VxFuture}

import scala.concurrent.Future
import scalaz.{\/, \/-}

package object utils {
  trait CirceRouteContinuations extends RouteContinuations with CirceVertxCodecs with ApiErrorSyntax {
    this: ScalaServiceVerticle =>
  }

  trait JacksonRouteContinuations extends RouteContinuations with JacksonVertxCodecs with ApiErrorSyntax {
    this: ScalaServiceVerticle =>
  }

  trait CirceRouteOperations extends RouteOperations with CirceVertxCodecs with ApiErrorSyntax {
    this: ScalaServiceVerticle =>
  }

  trait JacksonRouteOperations extends RouteOperations with JacksonVertxCodecs with ApiErrorSyntax {
    this: ScalaServiceVerticle =>
  }

  trait ApiErrorSyntax {
    type VEC = VertxExecutionContext

    // Operation
    implicit class OperationApiErrorConversion[E, A](o: Operation[E, A])(implicit f: scalaz.Functor[Future]) {
      def convertError(implicit c: ToApiError[E], ec: VEC): Operation[ApiError, A] = o.leftMap(c.toApiError)
    }

    // Future
    implicit class FutureDisjunctionApiErrorConversion[E, A](fut: Future[E \/ A])(implicit f: scalaz.Functor[Future]) {
      def convertError(implicit c: ToApiError[E], ec: VEC): Future[ApiError \/ A] = fut.map(_.leftMap(c.toApiError))
    }

    implicit class FutureEitherApiErrorConversion[E, A](fut: Future[Either[E, A]])(implicit f: scalaz.Functor[Future]) {
      def convertError(implicit c: ToApiError[E], ec: VEC): Future[Either[ApiError, A]] = fut.map(_.left.map(c.toApiError))
    }

    implicit class FutureApiErrorConversion[A](fut: Future[A])(implicit f: scalaz.Functor[Future]) {
      def convertError(implicit ec: VEC): Future[ApiError \/ A] = fut.map(\/-(_))
    }

    // VxFuture
    implicit class VxFutureDisjunctionApiErrorConversion[E, A](fut: VxFuture[E \/ A])(implicit f: scalaz.Functor[Future]) {
      def convertError(implicit c: ToApiError[E], ec: VEC): VxFuture[ApiError \/ A] = fut.compose(x => VxFuture.succeededFuture(x.leftMap(c.toApiError)))
    }

    implicit class VxFutureEitherApiErrorConversion[E, A](fut: VxFuture[Either[E, A]])(implicit f: scalaz.Functor[Future]) {
      def convertError(implicit c: ToApiError[E], ec: VEC): VxFuture[Either[ApiError, A]] = fut.compose(x => VxFuture.succeededFuture(x.left.map(c.toApiError)))
    }

    implicit class VxFutureApiErrorConversion[A](fut: VxFuture[A])(implicit f: scalaz.Functor[Future]) {
      def convertError(implicit ec: VEC): VxFuture[ApiError \/ A] = fut.compose(a => VxFuture.succeededFuture(\/-(a)))
    }

    // disjunctions
    implicit class DisjunctionApiErrorConversion[E, A](d: E \/ A)(implicit f: scalaz.Functor[Future]) {
      def convertError(implicit c: ToApiError[E]): ApiError \/ A = d.leftMap(c.toApiError)
    }

    implicit class EitherApiErrorConversion[E, A](d: Either[E, A])(implicit f: scalaz.Functor[Future]) {
      def convertError(implicit c: ToApiError[E]): Either[ApiError, A] = d.left.map(c.toApiError)
    }
  }

  trait ToApiError[A] {
    def toApiError(a: A): ApiError
  }
}
