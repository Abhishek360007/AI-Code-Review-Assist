package com.reviewassist.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Optional<Review> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);

    @Query("select avg(r.score) from Review r where r.owner.id = :ownerId")
    Double averageScore(@Param("ownerId") Long ownerId);

    @Query("select f.severity, count(f) from Review r join r.findings f where r.owner.id = :ownerId group by f.severity")
    List<Object[]> countFindingsBySeverity(@Param("ownerId") Long ownerId);

    @Query("select f.category, count(f) from Review r join r.findings f where r.owner.id = :ownerId group by f.category")
    List<Object[]> countFindingsByCategory(@Param("ownerId") Long ownerId);

    @Query("""
            select r.language, count(r), avg(r.score) from Review r
            where r.owner.id = :ownerId group by r.language order by count(r) desc
            """)
    List<Object[]> languageBreakdown(@Param("ownerId") Long ownerId);

    List<Review> findTop20ByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
