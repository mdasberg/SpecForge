package com.specforge.agent.service;

import com.specforge.agent.entity.RunnerKind;

/**
 * One way of checking a review head. A parser and a language model implement the same interface and
 * return the same result shape on purpose: which of the two produced a finding is provenance, not
 * structure, and the Checks tab should never have to be rebuilt because a check moved from one to
 * the other.
 */
interface CheckRunner {

    RunnerKind kind();

    CheckOutcome run(CheckContext context);
}
