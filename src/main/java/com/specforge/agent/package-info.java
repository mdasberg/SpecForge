/**
 * Automated and model-backed checks on a review's head, the findings they produce, and the accept,
 * dismiss and discuss handling.
 *
 * <p>The module publishes no API of its own: what the rest of the product needs from it is the
 * approval gate's {@code BlockingChecks} answer, and that port is declared by {@code approval}
 * precisely so a check runner cannot widen its own influence over the gate.
 *
 * <p>Agents comment and fail checks. They never approve, never resolve a human's thread, and never
 * dispose of a finding — those refusals live in the services that own each act.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Automated Review")
package com.specforge.agent;
