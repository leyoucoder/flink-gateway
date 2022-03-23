package cn.sliew.flink.gateway.engine;

import org.apache.flink.runtime.messages.webmonitor.JobIdsWithStatusOverview;
import org.apache.flink.runtime.messages.webmonitor.MultipleJobsDetails;
import org.apache.flink.runtime.rest.handler.async.AsynchronousOperationResult;
import org.apache.flink.runtime.rest.handler.async.TriggerResponse;
import org.apache.flink.runtime.rest.handler.legacy.messages.ClusterOverviewWithVersion;
import org.apache.flink.runtime.rest.messages.*;
import org.apache.flink.runtime.rest.messages.checkpoints.CheckpointConfigInfo;
import org.apache.flink.runtime.rest.messages.checkpoints.CheckpointStatistics;
import org.apache.flink.runtime.rest.messages.checkpoints.CheckpointingStatistics;
import org.apache.flink.runtime.rest.messages.checkpoints.TaskCheckpointStatisticsWithSubtaskDetails;
import org.apache.flink.runtime.rest.messages.dataset.ClusterDataSetListResponseBody;
import org.apache.flink.runtime.rest.messages.job.*;
import org.apache.flink.runtime.rest.messages.job.savepoints.SavepointDisposalRequest;
import org.apache.flink.runtime.rest.messages.job.savepoints.SavepointTriggerRequestBody;
import org.apache.flink.runtime.rest.messages.job.savepoints.stop.StopWithSavepointRequestBody;
import org.apache.flink.runtime.rest.messages.taskmanager.TaskManagerDetailsInfo;
import org.apache.flink.runtime.rest.messages.taskmanager.TaskManagersInfo;
import org.apache.flink.runtime.rest.messages.taskmanager.ThreadDumpInfo;
import org.apache.flink.runtime.webmonitor.handlers.*;
import org.apache.flink.runtime.webmonitor.threadinfo.JobVertexFlameGraph;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface RestEndpoint {

    /**
     * Returns the configuration of the WebUI.
     */
    DashboardConfiguration config() throws IOException;

    /**
     * Returns an overview over the Flink cluster.
     */
    ClusterOverviewWithVersion overview() throws IOException;

    /**
     * Shuts down the cluster
     */
    boolean shutdownCluster() throws IOException;

    /**
     * Returns all cluster data sets.
     */
    ClusterDataSetListResponseBody datasets() throws IOException;

    /**
     * Triggers the deletion of a cluster data set.
     * This async operation would return a 'triggerid' for further query identifier.
     */
    TriggerResponse deleteDataSet(String datasetId) throws IOException;

    /**
     * Returns the status for the delete operation of a cluster data set.
     */
    AsynchronousOperationResult deleteDataSetStatus(String triggerId) throws IOException;

    /**
     * Returns a list of all jars previously uploaded via '/jars/upload'.
     */
    JarListInfo jars() throws IOException;

    /**
     * Uploads a jar to the cluster.
     * The jar must be sent as multi-part data.
     * Make sure that the "Content-Type" header is set to "application/x-java-archive", as some http libraries do not add the header by default.
     * Using 'curl' you can upload a jar via 'curl -X POST -H "Expect:" -F "jarfile=@path/to/flink-job.jar" http://hostname:port/jars/upload'.
     */
    JarUploadResponseBody uploadJar(String filePath) throws IOException;

    /**
     * Deletes a jar previously uploaded via '/jars/upload'.
     *
     * @param jarId String value that identifies a jar. When uploading the jar a path is returned, where the filename is the ID. This value is equivalent to the `id` field in the list of uploaded jars (/jars).
     */
    boolean deleteJar(String jarId) throws IOException;

    /**
     * Returns the dataflow plan of a job contained in a jar previously uploaded via '/jars/upload'.
     * Program arguments can be passed both via the JSON request (recommended) or query parameters.
     *
     * @param jarId       String value that identifies a jar. When uploading the jar a path is returned, where the filename is the ID. This value is equivalent to the `id` field in the list of uploaded jars (/jars).
     * @param requestBody
     */
    JobPlanInfo jarPlan(String jarId, JarPlanRequestBody requestBody) throws IOException;

    /**
     * Submits a job by running a jar previously uploaded via '/jars/upload'.
     * Program arguments can be passed both via the JSON request (recommended) or query parameters.
     *
     * @param jarId String value that identifies a jar. When uploading the jar a path is returned, where the filename is the ID. This value is equivalent to the `id` field in the list of uploaded jars (/jars).
     * @param requestBody
     */
    JarRunResponseBody jarRun(String jarId, JarRunRequestBody requestBody) throws IOException;

    /**
     * Returns the cluster configuration.
     */
    List<ClusterConfigurationInfoEntry> jobmanagerConfig() throws IOException;

    /**
     * Returns the list of log files on the JobManager.
     */
    LogListInfo jobmanagerLogs() throws IOException;

    /**
     * Provides access to job manager metrics.
     */
    List<Map> jobmanagerMetrics(String get) throws IOException;

    /**
     * Returns an overview over all jobs and their current state.
     */
    JobIdsWithStatusOverview jobs() throws IOException;

    /**
     * Submits a job.
     * This call is primarily intended to be used by the Flink client.
     * This call expects a multipart/form-data request that consists of file uploads for the serialized JobGraph,
     * jars and distributed cache artifacts and an attribute named "request" for the JSON payload.
     */
    JobSubmitResponseBody jobSubmit(JobSubmitRequestBody requestBody) throws IOException;

    /**
     * Provides access to aggregated job metrics.
     */
    String jobsMetric(String get, String agg, String jobs) throws IOException;

    /**
     * Returns an overview over all jobs.
     */
    MultipleJobsDetails jobsOverview() throws IOException;

    /**
     * Returns details of a job.
     */
    JobDetailsInfo jobDetail(String jobId) throws IOException;

    /**
     * Terminates a job.
     */
    boolean jobTerminate(String jobId, String mode) throws IOException;

    /**
     * Returns the accumulators for all tasks of a job, aggregated across the respective subtasks.
     */
    JobAccumulatorsInfo jobAccumulators(String jobId, Boolean includeSerializedValue) throws IOException;

    /**
     * Returns checkpointing statistics for a job.
     */
    CheckpointingStatistics jobCheckpoints(String jobId) throws IOException;

    /**
     * Returns the checkpointing configuration.
     */
    CheckpointConfigInfo jobCheckpointConfig(String jobId) throws IOException;

    /**
     * Returns details for a checkpoint.
     */
    CheckpointStatistics jobCheckpointDetail(String jobId, String checkpointId) throws IOException;

    /**
     * Returns checkpoint statistics for a task and its subtasks.
     */
    TaskCheckpointStatisticsWithSubtaskDetails jobCheckpointSubtaskDetail(String jobId, String checkpointId, String vertexId) throws IOException;

    /**
     * Returns the configuration of a job.
     */
    String jobConfig(String jobId) throws IOException;

    /**
     * Returns the most recent exceptions that have been handled by Flink for this job.
     * The 'exceptionHistory.truncated' flag defines whether exceptions were filtered out through the GET parameter.
     * The backend collects only a specific amount of most recent exceptions per job.
     * This can be configured through web.exception-history-size in the Flink configuration.
     * The following first-level members are deprecated: 'root-exception', 'timestamp', 'all-exceptions', and 'truncated'.
     * Use the data provided through 'exceptionHistory', instead.
     */
    JobExceptionsInfoWithHistory jobException(String jobId, String maxExceptions) throws IOException;

    /**
     * Returns the result of a job execution.
     * Gives access to the execution time of the job and to all accumulators created by this job.
     */
    JobExecutionResultResponseBody jobExecutionResult(String jobId) throws IOException;

    /**
     * Provides access to job metrics.
     *
     * @param jobId         32-character hexadecimal string value that identifies a job.
     * @param get(optional) Comma-separated list of string values to select specific metrics.
     */
    String jobMetrics(String jobId, String get) throws IOException;

    /**
     * Returns the dataflow plan of a job.
     *
     * @param jobId 32-character hexadecimal string value that identifies a job.
     */
    JobPlanInfo jobPlan(String jobId) throws IOException;

    /**
     * Triggers the rescaling of a job. This async operation would return a 'triggerid' for further query identifier.
     *
     * @param jobId                   32-character hexadecimal string value that identifies a job.
     * @param parallelism(mandatory): Positive integer value that specifies the desired parallelism.
     */
    TriggerResponse jobRescale(String jobId, Integer parallelism) throws IOException;

    /**
     * Returns the status of a rescaling operation.
     *
     * @param jobId     32-character hexadecimal string value that identifies a job.
     * @param triggerId 32-character hexadecimal string that identifies an asynchronous operation trigger ID. The ID was returned then the operation was triggered.
     */
    AsynchronousOperationResult jobRescaleResult(String jobId, String triggerId) throws IOException;

    /**
     * Triggers a savepoint, and optionally cancels the job afterwards.
     * This async operation would return a 'triggerid' for further query identifier.
     *
     * @param jobId       32-character hexadecimal string value that identifies a job.
     * @param requestBody
     */
    TriggerResponse jobSavepoint(String jobId, SavepointTriggerRequestBody requestBody) throws IOException;

    /**
     * Returns the status of a savepoint operation.
     *
     * @param jobId     32-character hexadecimal string value that identifies a job.
     * @param triggerId 32-character hexadecimal string that identifies an asynchronous operation trigger ID. The ID was returned then the operation was triggered.
     */
    AsynchronousOperationResult jobSavepointResult(String jobId, String triggerId) throws IOException;

    /**
     * Stops a job with a savepoint.
     * Optionally, it can also emit a MAX_WATERMARK before taking the savepoint to flush out any state waiting for timers to fire.
     * This async operation would return a 'triggerid' for further query identifier.
     *
     * @param jobId       32-character hexadecimal string value that identifies a job.
     * @param requestBody
     */
    TriggerResponse jobStop(String jobId, StopWithSavepointRequestBody requestBody) throws IOException;

    /**
     * Returns details for a task, with a summary for each of its subtasks.
     *
     * @param jobId    32-character hexadecimal string value that identifies a job.
     * @param vertexId 32-character hexadecimal string value that identifies a job vertex.
     */
    JobVertexDetailsInfo jobVertexDetail(String jobId, String vertexId) throws IOException;

    /**
     * Returns user-defined accumulators of a task, aggregated across all subtasks.
     *
     * @param jobId    32-character hexadecimal string value that identifies a job.
     * @param vertexId 32-character hexadecimal string value that identifies a job vertex.
     */
    JobVertexAccumulatorsInfo jobVertexAccumulators(String jobId, String vertexId) throws IOException;

    /**
     * Returns back-pressure information for a job, and may initiate back-pressure sampling if necessary.
     *
     * @param jobId    32-character hexadecimal string value that identifies a job.
     * @param vertexId 32-character hexadecimal string value that identifies a job vertex.
     */
    JobVertexBackPressureInfo jobVertexBackPressure(String jobId, String vertexId) throws IOException;

    /**
     * Returns flame graph information for a vertex, and may initiate flame graph sampling if necessary.
     *
     * @param jobId          32-character hexadecimal string value that identifies a job.
     * @param vertexId       32-character hexadecimal string value that identifies a job vertex.
     * @param type(optional) String value that specifies the Flame Graph type. Supported options are: "[FULL, ON_CPU, OFF_CPU]".
     */
    JobVertexFlameGraph jobVertexFlameGraph(String jobId, String vertexId, String type) throws IOException;

    /**
     * Provides access to task metrics.
     *
     * @param jobId         32-character hexadecimal string value that identifies a job.
     * @param vertexId      32-character hexadecimal string value that identifies a job vertex.
     * @param get(optional) Comma-separated list of string values to select specific metrics.
     */
    String jobVertexMetrics(String jobId, String vertexId, String get) throws IOException;

    /**
     * Returns all user-defined accumulators for all subtasks of a task.
     *
     * @param jobId    32-character hexadecimal string value that identifies a job.
     * @param vertexId 32-character hexadecimal string value that identifies a job vertex.
     */
    SubtasksAllAccumulatorsInfo jobVertexSubtaskAccumulators(String jobId, String vertexId) throws IOException;


    /**
     * Provides access to aggregated subtask metrics.
     *
     * @param jobId              32-character hexadecimal string value that identifies a job.
     * @param vertexId           32-character hexadecimal string value that identifies a job vertex.
     * @param get(optional)      Comma-separated list of string values to select specific metrics.
     * @param agg(optional)      Comma-separated list of aggregation modes which should be calculated. Available aggregations are: "min, max, sum, avg".
     * @param subtasks(optional) Comma-separated list of integer ranges (e.g. "1,3,5-9") to select specific subtasks.
     */
    String jobVertexSubtaskMetrics(String jobId, String vertexId, String get, String agg, String subtasks) throws IOException;

    /**
     * Returns details of the current or latest execution attempt of a subtask.
     *
     * @param jobId        32-character hexadecimal string value that identifies a job.
     * @param vertexId     32-character hexadecimal string value that identifies a job vertex.
     * @param subtaskindex Positive integer value that identifies a subtask.
     */
    SubtaskExecutionAttemptDetailsInfo jobVertexSubtaskDetail(String jobId, String vertexId, Integer subtaskindex) throws IOException;

    /**
     * Returns details of an execution attempt of a subtask.
     * Multiple execution attempts happen in case of failure/recovery.
     *
     * @param jobId        32-character hexadecimal string value that identifies a job.
     * @param vertexId     32-character hexadecimal string value that identifies a job vertex.
     * @param subtaskindex Positive integer value that identifies a subtask.
     * @param attempt      Positive integer value that identifies an execution attempt.
     */
    SubtaskExecutionAttemptDetailsInfo jobVertexSubtaskAttemptDetail(String jobId, String vertexId, Integer subtaskindex, Integer attempt) throws IOException;

    /**
     * Returns the accumulators of an execution attempt of a subtask.
     * Multiple execution attempts happen in case of failure/recovery.
     *
     * @param jobId        32-character hexadecimal string value that identifies a job.
     * @param vertexId     32-character hexadecimal string value that identifies a job vertex.
     * @param subtaskindex Positive integer value that identifies a subtask.
     * @param attempt      Positive integer value that identifies an execution attempt.
     */
    SubtaskExecutionAttemptAccumulatorsInfo jobVertexSubtaskAttemptAccumulators(String jobId, String vertexId, Integer subtaskindex, Integer attempt) throws IOException;

    /**
     * Provides access to subtask metrics.
     *
     * @param jobId         32-character hexadecimal string value that identifies a job.
     * @param vertexId      32-character hexadecimal string value that identifies a job vertex.
     * @param subtaskindex  Positive integer value that identifies a subtask.
     * @param get(optional) Comma-separated list of string values to select specific metrics.
     */
    String jobVertexSubtaskMetrics(String jobId, String vertexId, Integer subtaskindex, String get) throws IOException;


    /**
     * Returns time-related information for all subtasks of a task.
     *
     * @param jobId    32-character hexadecimal string value that identifies a job.
     * @param vertexId 32-character hexadecimal string value that identifies a job vertex.
     */
    SubtasksTimesInfo jobVertexSubtaskTimes(String jobId, String vertexId) throws IOException;

    /**
     * Returns task information aggregated by task manager.
     *
     * @param jobId    32-character hexadecimal string value that identifies a job.
     * @param vertexId 32-character hexadecimal string value that identifies a job vertex.
     */
    JobVertexTaskManagersInfo jobVertexTaskManagers(String jobId, String vertexId) throws IOException;

    /**
     * Returns the watermarks for all subtasks of a task.
     *
     * @param jobId    32-character hexadecimal string value that identifies a job.
     * @param vertexId 32-character hexadecimal string value that identifies a job vertex.
     */
    String jobVertexWatermarks(String jobId, String vertexId) throws IOException;

    /**
     * Triggers the desposal of a savepoint. This async operation would return a 'triggerid' for further query identifier.
     *
     * @param request
     */
    TriggerResponse savepointDisposal(SavepointDisposalRequest request) throws IOException;

    /**
     * Returns the status of a savepoint disposal operation.
     *
     * @param triggerId 32-character hexadecimal string that identifies an asynchronous operation trigger ID. The ID was returned then the operation was triggered.
     */
    AsynchronousOperationResult savepointDisposalResult(String triggerId) throws IOException;

    /**
     * Returns an overview over all task managers.
     */
    TaskManagersInfo taskManagersDetail() throws IOException;

    /**
     * Provides access to aggregated task manager metrics.
     *
     * @param get(optional)          Comma-separated list of string values to select specific metrics.
     * @param agg(optional)          Comma-separated list of aggregation modes which should be calculated. Available aggregations are: "min, max, sum, avg".
     * @param taskmanagers(optional) Comma-separated list of 32-character hexadecimal strings to select specific task managers.
     */
    String taskManagersMetrics(String get, String agg, String taskmanagers) throws IOException;

    /**
     * Returns details for a task manager.
     * "metrics.memorySegmentsAvailable" and "metrics.memorySegmentsTotal" are deprecated.
     * Please use "metrics.nettyShuffleMemorySegmentsAvailable" and "metrics.nettyShuffleMemorySegmentsTotal" instead.
     *
     * @param taskManagerId 32-character hexadecimal string that identifies a task manager.
     */
    TaskManagerDetailsInfo taskManagerDetail(String taskManagerId) throws IOException;

    /**
     * Returns the list of log files on a TaskManager.
     *
     * @param taskManagerId 32-character hexadecimal string that identifies a task manager.
     */
    LogListInfo taskManagerLogs(String taskManagerId) throws IOException;

    /**
     * Provides access to task manager metrics.
     *
     * @param taskManagerId 32-character hexadecimal string that identifies a task manager.
     * @param get(optional) Comma-separated list of string values to select specific metrics.
     */
    String taskManagerMetrics(String taskManagerId, String get) throws IOException;

    /**
     * Returns the thread dump of the requested TaskManager.
     *
     * @param taskManagerId 32-character hexadecimal string that identifies a task manager.
     */
    ThreadDumpInfo taskManagerThreadDump(String taskManagerId) throws IOException;


}
