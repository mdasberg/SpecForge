package com.specforge.platform.identity;

import com.specforge.platform.MemberRef;
import com.specforge.platform.Members;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
class MembersImpl implements Members {

    private final UserRepository users;

    @Override
    public Optional<MemberRef> byHandle(final String handle) {
        return users.findByHandle(handle).map(MembersImpl::ref);
    }

    @Override
    public Map<String, MemberRef> byIds(final Collection<String> subjectIds) {
        return users.findAllById(subjectIds).stream().collect(Collectors.toMap(User::subjectId, MembersImpl::ref));
    }

    @Override
    public List<MemberRef> search(final String query, final int limit) {
        return users.search(query).stream().limit(limit).map(MembersImpl::ref).toList();
    }

    private static MemberRef ref(final User user) {
        return new MemberRef(user.subjectId(), user.displayName(), user.handle(), user.avatarUrl(), user.actorKind());
    }
}
