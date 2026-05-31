package com.pavlent1yy.gradinator.repository;

import com.pavlent1yy.gradinator.dto.ScheduleFullProjection;
import com.pavlent1yy.gradinator.dto.ScheduleProjection;
import com.pavlent1yy.gradinator.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
//    @Query("""
//        SELECT s.pairNumber AS pairNumber,
//               s.subject AS subject,
//               s.teacher AS teacher,
//               s.room AS room
//        FROM Schedule s
//        WHERE s.studentGroup.id = :groupId
//          AND s.weekDay.id = :weekdayId
//    """)
//    List<ScheduleProjection> findScheduleByGroupIdAndWeekdayIdAndWeekTypeIds(
//            @Param("groupId") Long groupId,
//            @Param("weekdayId") Long weekdayId,
//            @Param("weektypeIds") List<Long> weektypeIds
//    );

//    @Query("""
//    SELECT wd.name AS weekday,
//           s.pairNumber AS pairNumber,
//           s.subject AS subject,
//           s.teacher AS teacher,
//           s.room AS room
//    FROM Schedule s
//    JOIN s.group g
//    JOIN s.weekType wt
//    JOIN s.weekDay wd
//    WHERE g.name = :groupName
//      AND wt.name IN :weektypeNames
//""")
//    List<ScheduleFullProjection> findScheduleByGroupNameAndWeektypeName(
//            @Param("groupName") String groupName,
//            @Param("weektypeNames") String weektypeNames
//    );
}
