package com.specforge.agent.api;

import com.specforge.agent.service.CheckService;
import com.specforge.agent.service.FindingService;
import com.specforge.platform.Callers;
import com.specforge.platform.api.dto.CheckRunList;
import com.specforge.platform.api.dto.DiscussFindingRequest;
import com.specforge.platform.api.dto.Finding;
import com.specforge.platform.api.generated.ChecksApi;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * The automated-review capability's HTTP surface, implementing the interface generated from
 * {@code specforge-api.yaml}. There is no mapping here: the services speak the contract's types and
 * this class only routes. Whether an agent may dispose of a finding is a rule, not an authority
 * level, so it lives in the service beside the rest of them rather than in an annotation here.
 */
@RequiredArgsConstructor
@RestController
class ChecksController implements ChecksApi {

    private final CheckService checks;
    private final FindingService findings;
    private final Callers callers;

    @Override
    public CheckRunList listChecks(final UUID reviewId) {
        return checks.list(reviewId);
    }

    @Override
    public CheckRunList rerunChecks(final UUID reviewId) {
        return checks.rerunAll(reviewId);
    }

    @Override
    public CheckRunList rerunCheck(final UUID checkRunId) {
        return checks.rerun(checkRunId);
    }

    @Override
    public Finding acceptFinding(final UUID findingId) {
        return findings.accept(findingId, callers.current());
    }

    @Override
    public Finding dismissFinding(final UUID findingId) {
        return findings.dismiss(findingId, callers.current());
    }

    @Override
    public Finding discussFinding(final UUID findingId, final DiscussFindingRequest discussFindingRequest) {
        return findings.discuss(findingId, discussFindingRequest, callers.current());
    }

    @Override
    public Finding undoFindingDisposition(final UUID findingId) {
        return findings.undo(findingId, callers.current());
    }
}
