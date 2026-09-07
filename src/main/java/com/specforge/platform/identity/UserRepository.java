package com.specforge.platform.identity;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByHandle(String handle);

    @Query("select u from User u where lower(u.handle) like lower(concat(:query, '%'))"
            + " or lower(u.displayName) like lower(concat(:query, '%')) order by u.displayName")
    List<User> search(@Param("query") String query);
}
