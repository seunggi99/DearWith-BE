package com.dearwith.dearwith_backend.event.repository;

import com.dearwith.dearwith_backend.event.entity.Event;
import com.dearwith.dearwith_backend.event.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findTop10ByOrderByCreatedAtDesc();

    @Query("""
        select e
        from Event e
        where e.status <> :excludedStatus
          and e.id not in :excludedIds
        order by e.bookmarkCount desc
    """)
    List<Event> findTopByStatusNotAndIdNotInOrderByBookmarkCountDesc(
            @Param("excludedStatus") EventStatus excludedStatus,
            @Param("excludedIds") List<Long> excludedIds,
            Pageable pageable
    );

    @Modifying
    @Query("update Event e set e.bookmarkCount = e.bookmarkCount + 1 where e.id = :eventId")
    int incrementBookmark(@Param("eventId") Long eventId);

    @Modifying
    @Query("""
           update Event e
           set e.bookmarkCount = case when e.bookmarkCount > 0 then e.bookmarkCount - 1 else 0 end
           where e.id = :eventId
           """)
    int decrementBookmark(@Param("eventId") Long eventId);

    @Query("""
    SELECT e FROM Event e
    WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%'))
""")
    Page<Event> searchByTitle(@Param("query") String query, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Event e
           set e.status = com.dearwith.dearwith_backend.event.enums.EventStatus.IN_PROGRESS
         where e.status <> com.dearwith.dearwith_backend.event.enums.EventStatus.IN_PROGRESS
           and e.startDate <= :now
           and e.endDate   >= :now
           and e.deletedAt is null
    """)
    int bulkMarkInProgress(@Param("now") LocalDate now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Event e
           set e.status = com.dearwith.dearwith_backend.event.enums.EventStatus.ENDED
         where e.status <> com.dearwith.dearwith_backend.event.enums.EventStatus.ENDED
           and e.endDate < :now
           and e.deletedAt is null
    """)
    int bulkMarkEnded(@Param("now") LocalDate now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Event e
           set e.status = com.dearwith.dearwith_backend.event.enums.EventStatus.SCHEDULED
         where e.status <> com.dearwith.dearwith_backend.event.enums.EventStatus.SCHEDULED
           and e.startDate > :now
           and e.deletedAt is null
    """)
    int bulkMarkScheduled(@Param("now") LocalDate now);

    @Query("""
        select distinct e
          from Event e
          join e.artists eam
         where eam.artist.id = :artistId
           and e.deletedAt is null
    """)
    Page<Event> findPageByArtistId(@Param("artistId") Long artistId, Pageable pageable);

    @Query("""
    select distinct e
    from Event e
        join e.artistGroups eag
    where
        eag.artistGroup.id = :groupId
    """)
    Page<Event> findPageByGroup(@Param("groupId") Long groupId, Pageable pageable);

    @Query("select e.bookmarkCount from Event e where e.id = :eventId")
    long getBookmarkCount(@Param("eventId") Long eventId);
    List<Event> findByIdIn(Collection<Long> ids);

    @Query("""
        select distinct e
        from Event e
            left join e.artists ea       
            left join ea.artist a
            left join e.artistGroups eag   
            left join eag.artistGroup g
       where
            (a.id in :artistIds or g.id in :groupIds)
            and e.status in (
                com.dearwith.dearwith_backend.event.enums.EventStatus.SCHEDULED,
                com.dearwith.dearwith_backend.event.enums.EventStatus.IN_PROGRESS
            )
        order by
            e.startDate asc nulls last,
            e.bookmarkCount desc,
            e.id desc
        """)
    Page<Event> findRecommendedForUser(
            @Param("artistIds") List<Long> artistIds,
            @Param("groupIds") List<Long> groupIds,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @Query("""
    select e
    from Event e
    where
        e.status in (
            com.dearwith.dearwith_backend.event.enums.EventStatus.SCHEDULED,
            com.dearwith.dearwith_backend.event.enums.EventStatus.IN_PROGRESS
        )
        and (e.endDate is null or e.endDate >= :today)
    order by
        case e.status
            when com.dearwith.dearwith_backend.event.enums.EventStatus.IN_PROGRESS then 0
            else 1
        end,
        e.startDate asc nulls last,  
        e.bookmarkCount desc,        
        e.createdAt desc              
    """)
    List<Event> findGlobalRecommendedFallback(@Param("today") LocalDate today, Pageable pageable);

    Page<Event> findByUser_Id(UUID userId, Pageable pageable);

    @Query("""
        select distinct e from Event e
        left join fetch e.coverImage ci
        left join fetch e.artists eam
        left join fetch eam.artist a
        left join fetch a.profileImage aImg
        left join fetch e.artistGroups egm
        left join fetch egm.artistGroup g
        left join fetch g.profileImage gImg
        where e.id in :ids
        """)
    List<Event> findWithMainPageRelationsByIdIn(@Param("ids") List<Long> ids);
}
