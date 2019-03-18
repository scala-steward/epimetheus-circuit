package io.chrisdavenport.epimetheus.circuit

import cats.effect._
import cats.implicits._
import io.chrisdavenport.epimetheus._
import io.chrisdavenport.circuit._
import shapeless._


abstract class RejectedExecutionCounter[F[_]]{
  def circuitBreaker(c: CircuitBreaker[F], circuitName: String): CircuitBreaker[F]
}

object RejectedExecutionCounter {

  def single[F[_]: Sync](
    cr: CollectorRegistry[F],
    metricName: Name,
    circuit: CircuitBreaker[F]
  ): F[CircuitBreaker[F]] = 
    Counter.noLabels[F](cr, metricName, "Circuit Breaker Rejected Executions.")
      .map(counter => circuit.doOnRejected(counter.inc))

  def register[F[_]: Sync](
    cr: CollectorRegistry[F],
    metricName: Name = Name("circuit_rejected_execution_total")
  ): F[RejectedExecutionCounter[F]] =
    Counter.labelled(
      cr,
      metricName,
      "Circuit Breaker Rejected Executions.",
      Sized(Name("circuit_name")),
      {s: String => Sized(s)}
    ).map(new DefaultRejectedExecutionCounter(_))

  private class DefaultRejectedExecutionCounter[F[_]](
    counter: UnlabelledCounter[F, String]
  ) extends RejectedExecutionCounter[F]{
    def circuitBreaker(c: CircuitBreaker[F], circuitName: String): CircuitBreaker[F] = 
      c.doOnRejected(counter.label(circuitName).inc)
  }

}