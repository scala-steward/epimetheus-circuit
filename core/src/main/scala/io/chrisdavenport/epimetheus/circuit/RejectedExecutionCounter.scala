package io.chrisdavenport.epimetheus.circuit

import cats.effect._
import cats.implicits._
import io.chrisdavenport.epimetheus._
import io.chrisdavenport.circuit._
import shapeless._


abstract class RejectedExecutionCounter[F[_]]{
  def meteredCircuit(c: CircuitBreaker[F], circuitName: String): CircuitBreaker[F]
}

object RejectedExecutionCounter {

  /**
   * Initialization of the Generalized Modifier
   * which can be applied to multiple circuit breakers.
   */
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
    override def meteredCircuit(c: CircuitBreaker[F], circuitName: String): CircuitBreaker[F] = 
      c.doOnRejected(counter.label(circuitName).inc)
  }


  /**
   * Single Metered Circuit
   */
  def meteredCircuit[F[_]: Sync](
    cr: CollectorRegistry[F],
    metricName: Name,
    circuit: CircuitBreaker[F]
  ): F[CircuitBreaker[F]] = 
    Counter.noLabels[F](cr, metricName, "Circuit Breaker Rejected Executions.")
      .map(counter => circuit.doOnRejected(counter.inc))

}