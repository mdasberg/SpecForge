package com.specforge.platform;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The identities the rest of the product may address by handle, offered to any capability that has
 * to resolve an {@code @}-mention or list candidates for one — mentions and notifications, above
 * all — without owning a second copy of the identity mirror.
 */
public interface Members {

    /** The member with this handle, if that handle has ever been mirrored from a token. */
    Optional<MemberRef> byHandle(String handle);

    /** Names the given subjects, skipping any that have never been mirrored. */
    Map<String, MemberRef> byIds(Collection<String> subjectIds);

    /** Handle or display name starting with {@code query}, for mention autocomplete. */
    List<MemberRef> search(String query, int limit);
}
