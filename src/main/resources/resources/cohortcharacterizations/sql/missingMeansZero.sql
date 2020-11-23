INSERT INTO #events_count
select person_id, event_id, cast(0 as int) as value_as_number
from #qualified_events q
where not exists (select 1 from #events_count ec where q.person_id = ec.person_id and q.event_id = ec.event_id);
