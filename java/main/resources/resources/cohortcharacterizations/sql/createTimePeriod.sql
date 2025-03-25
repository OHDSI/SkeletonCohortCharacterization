IF OBJECT_ID('tempdb..#time_period', 'U') IS NOT NULL
    DROP TABLE #time_period;
CREATE TABLE #time_period(
    time_id int,
    start_day int,
    end_day int
);

INSERT INTO #time_period(time_id, start_day, end_day) VALUES @values;