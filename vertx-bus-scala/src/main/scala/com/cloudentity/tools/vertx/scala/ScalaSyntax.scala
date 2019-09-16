package com.cloudentity.tools.vertx.scala

import scalaz.Monad
import scalaz.std.FutureInstances
import scalaz.syntax._
import scalaz.syntax.std.{ToListOps, ToOptionOps}

import scala.concurrent.Future

trait ScalaSyntax extends ToIdOps with ToEitherOps with ToOptionOps with ToListOps with FutureInstances {
  type EC = VertxExecutionContext
  type \/[A, B] = scalaz.\/[A, B]
  type \/-[B] = scalaz.\/-[B]
  type -\/[A] = scalaz.-\/[A]
  type VxFuture[A] = io.vertx.core.Future[A]

  implicit class SafeContains[T](x: Seq[T]) {
    def has(t: T): Boolean = x.contains(t)
  }

  implicit class SafeEquals[A](a: A) {
    def ===[B <: A](x: B): Boolean = a == x
  }

  implicit class SafeEqualsSuper[A](a: A) {
    def ===[B >: A](x: B): Boolean = a == x
  }

  implicit class TraverseListOperationSyntax[A](a: List[A]) {
    def traverseOperations[E, B](f: A => Operation[E, B])(implicit M: Monad[Operation[E, ?]]): Operation[E, List[B]] =
      Operation.traverse(a)(f)

    def traverseOperationsF[E, B](f: A => Future[E \/ B])(implicit M: Monad[Operation[E, ?]]): Operation[E, List[B]] =
      Operation.traverseEitherF(a)(f)

    def traverseOperationsV[E, B](f: A => VxFuture[E \/ B])(implicit M: Monad[Operation[E, ?]], ec: EC): Operation[E, List[B]] =
      Operation.traverseEitherV(a)(f)
  }

  implicit class EitherOperationSyntax[E, A](either: E \/ A) {
    def toOperation: Operation[E, A] = Operation.fromEither(either)
  }

  implicit class StdEitherOperationSyntax[E, A](either: Either[E, A]) {
    def toOperation: Operation[E, A] = Operation.fromStdEither(either)
  }

  implicit class OptionOperationSyntax[A](option: Option[A]) {
    def toOperation[E](onEmpty: => E)(implicit ec: EC): Operation[E, A] = Operation.fromOption(onEmpty)(option)
  }

  implicit class FutureOperationSyntax[A](future: Future[A]) {
    def toOperation[E](implicit ec: EC): Operation[E, A] = Operation.fromF(future)
  }

  implicit class VxFutureOperationSyntax[A](vxFuture: VxFuture[A]) {
    def toOperation[E](implicit ec: EC): Operation[E, A] = Operation.fromV(vxFuture)
  }

  implicit class VxFutureOptionOperationSyntax[A](vxFuture: VxFuture[Option[A]]) {
    def toOperationOrElse[E](onEmpty: => E)(implicit ec: EC): Operation[E, A] = Operation.fromOptionV(onEmpty)(vxFuture)
  }

  implicit class VxFutureEitherOperationSyntax[E, A](vxFuture: VxFuture[E \/ A]) {
    def toOperation(implicit ec: EC): Operation[E, A] = Operation.fromEitherV(vxFuture)
    def `¯\\_(ツ)_/¯`(implicit ec: EC): Operation[E, A] = Operation.fromEitherV(vxFuture)
  }

  implicit class VxFutureStdEitherOperationSyntax[E, A](vxFuture: VxFuture[Either[E, A]]) {
    def toOperation(implicit ec: EC): Operation[E, A] = Operation.fromStdEitherV(vxFuture)
    def `¯\\_(ツ)_/¯`(implicit ec: EC): Operation[E, A] = Operation.fromStdEitherV(vxFuture)
  }

  implicit class FutureEitherOperationSyntax[E, A](future: Future[E \/ A]) {
    def toOperation: Operation[E, A] = Operation.fromEitherF(future)
  }

  implicit class FutureStdEitherOperationSyntax[E, A](future: Future[Either[E, A]]) {
    def toOperation(implicit ec: EC): Operation[E, A] = Operation.fromStdEitherF(future)
  }

  implicit class FutureOptionOperationSyntax[A](option: Future[Option[A]]) {
    def toOperationOrElse[E](onEmpty: => E)(implicit ec: EC): Operation[E, A] = Operation.fromOptionF(onEmpty)(option)
  }
}

object syntax extends ScalaSyntax
