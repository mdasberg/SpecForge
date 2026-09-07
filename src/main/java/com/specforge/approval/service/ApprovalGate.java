package com.specforge.approval.service;

import com.specforge.approval.CheckState;
import com.specforge.approval.entity.RequiredReviewerEntity;
import com.specforge.approval.entity.ReviewerState;
import com.specforge.repository.ApprovalRule;
import java.util.ArrayList;
import java.util.List;

/**
 * The rule and the gate it feeds, computed fresh from the review's live seats every time — one
 * source for the state and the sentence that explains it, so the panel and the API can never
 * disagree about why a review is or is not approved.
 */
final class ApprovalGate {

    private ApprovalGate() {}

    record RuleState(boolean satisfied, int approvedCount, int requiredCount, String reason) {}

    record Result(boolean passed, RuleState rule, int unresolvedBlockingThreads, CheckState checks, List<String> reasons) {}

    static RuleState rule(final List<RequiredReviewerEntity> seats, final ApprovalRule rule) {
        final int approvedCount = (int) seats.stream().filter(s -> s.state() == ReviewerState.APPROVED).count();
        final List<String> missingRoles = new ArrayList<>();
        for (final String role : rule.requiredRoles()) {
            final boolean covered = seats.stream()
                    .anyMatch(s -> role.equals(s.requiredRole()) && s.state() == ReviewerState.APPROVED);
            if (!covered) {
                missingRoles.add(role);
            }
        }
        final boolean satisfied = approvedCount >= rule.minApprovals() && missingRoles.isEmpty();
        return new RuleState(satisfied, approvedCount, rule.minApprovals(), reason(satisfied, approvedCount, rule, missingRoles));
    }

    static Result gate(
            final List<RequiredReviewerEntity> seats,
            final ApprovalRule rule,
            final int unresolvedBlockingThreads,
            final CheckState checks) {
        final RuleState ruleState = rule(seats, rule);
        final boolean checksFailing = checks.configured() && checks.failed();
        final List<String> reasons = new ArrayList<>();
        if (!ruleState.satisfied()) {
            reasons.add(ruleState.reason());
        }
        if (unresolvedBlockingThreads > 0) {
            reasons.add("%d blocking conversation%s unresolved.".formatted(
                    unresolvedBlockingThreads, unresolvedBlockingThreads == 1 ? "" : "s"));
        }
        if (checksFailing) {
            reasons.add("Failing check%s: %s.".formatted(
                    checks.failedCheckNames().size() == 1 ? "" : "s", String.join(", ", checks.failedCheckNames())));
        }
        final boolean passed = ruleState.satisfied() && unresolvedBlockingThreads == 0 && !checksFailing;
        return new Result(passed, ruleState, unresolvedBlockingThreads, checks, reasons);
    }

    private static String reason(
            final boolean satisfied, final int approvedCount, final ApprovalRule rule, final List<String> missingRoles) {
        if (satisfied) {
            return "%d of %d required approvals; the rule is met.".formatted(approvedCount, rule.minApprovals());
        }
        final StringBuilder reason = new StringBuilder(
                "%d of %d required approvals".formatted(approvedCount, rule.minApprovals()));
        if (!missingRoles.isEmpty()) {
            reason.append("; needs one from ").append(roleList(missingRoles));
        }
        reason.append('.');
        return reason.toString();
    }

    private static String roleList(final List<String> roles) {
        return roles.size() == 1 ? "a " + roles.get(0).toLowerCase() : String.join(" and ", roles);
    }
}
