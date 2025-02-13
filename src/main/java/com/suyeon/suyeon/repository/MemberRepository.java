package com.suyeon.suyeon.repository;

import com.suyeon.suyeon.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByUsername(String username);
}
