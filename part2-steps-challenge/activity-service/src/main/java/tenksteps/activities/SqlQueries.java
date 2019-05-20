package tenksteps.activities;

interface SqlQueries {

  static String insertStepEvent() {
    return "INSERT INTO StepEvent VALUES($1, $2, current_timestamp, $3)";
  }

  static String stepsCountForToday() {
    return "SELECT current_timestamp, coalesce(sum(steps_count), 0) FROM StepEvent WHERE " +
      "(device_id = $1) AND" +
      "(date_trunc('day', sync_timestamp) = date_trunc('day', current_timestamp))";
  }
}
