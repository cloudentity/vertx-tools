package com.cloudentity.tools.vertx.scala

import com.cloudentity.tools.vertx.scala.Operation.EC

import scala.concurrent.Future
import io.vertx.core.{Future => VxFuture}

import scalaz._
import Scalaz._
import scalaz.std.FutureInstances

case class Operation[E, A](op: EitherT[Future, E, A]) {
  def run: Future[E \/ A] = op.run

  /** Map over the right of either */
  def map[B](f: A => B)(implicit M: Monad[Operation[E, ?]]): Operation[E, B] =
    M.map(this)(f)

  /** Flatmap over the right of either */
  def flatMap[B](f: A => Operation[E, B])(implicit M: Monad[Operation[E, ?]]): Operation[E, B] =
    M.bind(this)(f)

  /** Flatmap over the right of either using either */
  def flatMapE[B](f: A => E \/ B)(implicit M: Monad[Future]): Operation[E, B] =
    flatMapF(a => Future.successful(f(a)))

  /** Flatmap over the right of either using future of either */
  def flatMapF[B](f: A => Future[E \/ B])(implicit M: Monad[Future]): Operation[E, B] =
    Operation(op.flatMapF(f))

  /** Map over the left of either */
  def leftMap[F](f: E => F)(implicit M: Functor[Future]): Operation[F, A] =
    Operation(op.leftMap(f))

  /** Recover the failed future to either */
  def recover(f: Throwable => \/[E, A])(implicit ec: EC): Operation[E, A] = op.run.recover {
    case t => f(t)
  } |> Operation.fromEitherF

  /** Recover the failed future to future */
  def recoverWith(f: Throwable => Future[\/[E, A]])(implicit ec: EC): Operation[E, A] = op.run.recoverWith {
    case t => f(t)
  } |> Operation.fromEitherF

  /** Recover the left of either to right */
  def recoverError(f: E => A)(implicit ec: EC): Operation[E, A] = op.run.map(_.recover {
    case e => f(e)
  }) |> Operation.fromEitherF

  /** Recover the left of either to either */
  def recoverErrorWith(f: E => \/[E, A])(implicit ec: EC): Operation[E, A] = op.run.map(_.recoverWith[E, A] {
    case t => f(t)
  }) |> Operation.fromEitherF

  def recoverErrorWithF(f: E => Future[\/[E, A]])(implicit ec: EC): Operation[E, A] = op.run.flatMap {
    case -\/(e) => f(e)
    case \/-(a) => Future.successful(\/-(a))
  } |> Operation.fromEitherF

  def recoverErrorWithO(f: E => Operation[E, A])(implicit ec: EC): Operation[E, A] =
    recoverErrorWithF(f.andThen(_.run))

  /** Return this if it is a right, otherwise, return the given value */
  def orElse(x: => Operation[E, A])(implicit M: Monad[Future]): Operation[E, A] = Operation(op.orElse(x.op))

  /** Return the right value of this disjunction or run the given function on the left. */
  def valueOr(x: E => A)(implicit F: Functor[Future]): Future[A] = op.valueOr(x)

  /** Return the right value of this disjunction or the given default if left */
  def getOrElse(default: => A)(implicit F: Functor[Future]): Future[A] = op.getOrElse(default)
}

object Operation extends FutureConversions with FutureInstances {
  type EC = VertxExecutionContext

  /** Construct successful value */
  def success[E, A](a: A): Operation[E, A] =
    from(a)

  /** Construct error as left either */
  def error[E, A](e: E): Operation[E, A] =
    fromEither(-\/(e))

  /** Construct failed future */
  def failure[E, A](t: Throwable): Operation[E, A] =
    fromEitherF(Future.failed(t))

  /** Construct empty operation */
  def empty[E]: Operation[E, Unit] =
    from(())

  /** Another way to construct a successful value */
  def from[E, A](a: A): Operation[E, A] =
    fromEither(\/-(a))

  /** Construct operation from either */
  def fromEither[E, A](either: E \/ A): Operation[E, A] =
    fromEitherF(Future.successful(either))

  /** Construct operation from standard either */
  def fromStdEither[E, A](either: Either[E, A]): Operation[E, A] =
    fromEitherF(Future.successful(\/.fromEither(either)))

  /** Construct operation from future of either */
  def fromEitherF[E, A](eitherF: Future[E \/ A]): Operation[E, A] =
    Operation(EitherT(eitherF))

  /** Construct operation from future of standard either */
  def fromStdEitherF[E, A](eitherF: Future[Either[E, A]])(implicit ec: EC): Operation[E, A] =
    Operation(EitherT(eitherF.map(\/.fromEither)))

  /** Construct operation from vertx future of either */
  def fromEitherV[E, A](eitherV: VxFuture[E \/ A])(implicit ec: EC) : Operation[E, A] =
    fromEitherF(eitherV.toScala())

  /** Construct operation from vertx future of standard either */
  def fromStdEitherV[E, A](eitherV: VxFuture[Either[E, A]])(implicit ec: EC) : Operation[E, A] =
    fromStdEitherF(eitherV.toScala())

  /** Construct operation from option and fallback to error if none */
  def fromOption[E, A](onEmpty: => E)(o: Option[A]): Operation[E, A] =
    fromEither(o.map(_.right).getOrElse(onEmpty.left))

  /** Construct operation from future option and fallback to error if none */
  def fromOptionF[E, A](onEmpty: => E)(o: Future[Option[A]])(implicit ec: EC): Operation[E, A] =
    fromF(o).flatMap(fromOption(onEmpty))

  /** Construct operation from vertx future option and fallback to error if none */
  def fromOptionV[E, A](onEmpty: => E)(o: VxFuture[Option[A]])(implicit ec: EC): Operation[E, A] =
    fromV(o).flatMap(fromOption(onEmpty))

  /** Construct operation from future of value */
  def fromF[E, A](f: Future[A])(implicit ec: EC): Operation[E, A] =
    fromEitherF(f.map(_.right))

  /** Construct operation from vertx future of value */
  def fromV[E, A](v: VxFuture[A])(implicit ec: EC): Operation[E, A] =
    fromF[E, A](v.toScala())

  /** Construct operation from future. If a future is failed, recover it to either left using handler */
  def handleF[E, A](handler: Throwable => E)(f: Future[A])(implicit ec: EC): Operation[E, A] =
    fromEitherF(f.transformWith(t => Future.successful(Disjunction.fromEither(t.toEither).leftMap(handler))))

  /** Construct operation from vertx future. If a future is failed, recover it to either left using handler */
  def handleV[E, A](handler: Throwable => E)(v: VxFuture[A])(implicit ec: EC): Operation[E, A] =
    handleF(handler)(v.toScala())

  /** For Operation of Option get the value if exists, otherwise fail with error */
  def getOrElse[E, A](onEmpty: => E)(op: Operation[E, Option[A]])(implicit M: Monad[Future]) = op.flatMapE {
    case Some(a) => \/-(a)
    case None    => -\/(onEmpty)
  }

  /** Traverse the list l using f function. It stops on a first error or failed future. */
  def traverse[E, A, B](l: List[A])(f: A => Operation[E, B])
                       (implicit M: Monad[Operation[E, ?]]): Operation[E, List[B]] =
    l.traverse[Operation[E, ?], B](f)

  /** Traverse the list l using f function. It stops on a first error or failed future. */
  def traverseEitherF[E, A, B](l: List[A])(f: A => Future[E \/ B])
                              (implicit M: Monad[Operation[E, ?]]): Operation[E, List[B]] =
    traverse(l)(a => fromEitherF(f(a)))

  /** Traverse the list l using f function It stops on a first error or failed future. */
  def traverseEitherV[E, A, B](l: List[A])(f: A => VxFuture[E \/ B])
                              (implicit M: Monad[Operation[E, ?]], ec: EC): Operation[E, List[B]] =
    traverse(l)(a => fromEitherV(f(a)))

  /** Traverse the list l using f function. Collect all errors. It fails on first failed future */
  def traverseAndCollect[E, A, B](l: List[A])(f: A => Operation[E, B])(implicit ec: EC): Future[(List[E], List[B])] =
    Future.traverse(l)(a => f(a).op.run).map(_.separate)

  /** Run two operation in parallel. It fails on first error or failed future */
  def parallel[E, A, B](a: Operation[E, A], b: Operation[E, B])
                       (implicit M: Monad[Operation[E, ?]]): Operation[E, (A, B)] =
    M.tuple2(a, b)

  /** Run three operation in parallel. It fails on first error or failed future */
  def parallel3[E, A, B, C](a: Operation[E, A], b: Operation[E, B], c: Operation[E, C])
                           (implicit M: Monad[Operation[E, ?]]): Operation[E, (A, B, C)] =
    M.tuple3(a, b, c)

  /** Run four operation in parallel. It fails on first error or failed future */
  def parallel4[E, A, B, C, D](a: Operation[E, A], b: Operation[E, B], c: Operation[E, C], d: Operation[E, D])
                              (implicit M: Monad[Operation[E, ?]]): Operation[E, (A, B, C, D)] =
    M.tuple4(a, b, c, d)

  implicit def monad[E](implicit ec: EC): Monad[Operation[E, ?]] = new Monad[Operation[E, ?]] {
    val M: Monad[Future] = implicitly[Monad[Future]]

    override def point[A](a: => A): Operation[E, A] =
      Operation(EitherT[Future, E, A](M.point(a.right[E])))

    override def bind[A, B](fa: Operation[E, A])(f: A => Operation[E, B]): Operation[E, B] =
      Operation(EitherT[Future, E, B](M.bind(fa.run) {
        case \/-(a) => f(a).run
        case -\/(e) => Future.successful(-\/(e))
      }))
  }
}
