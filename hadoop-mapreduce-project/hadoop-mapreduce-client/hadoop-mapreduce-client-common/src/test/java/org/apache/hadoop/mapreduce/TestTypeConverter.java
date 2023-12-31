/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.mapreduce;

import org.apache.hadoop.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.JobStatus.State;
import org.apache.hadoop.mapreduce.v2.api.records.JobId;
import org.apache.hadoop.mapreduce.v2.api.records.JobReport;
import org.apache.hadoop.mapreduce.v2.api.records.JobState;
import org.apache.hadoop.mapreduce.v2.api.records.TaskState;
import org.apache.hadoop.mapreduce.v2.api.records.TaskType;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationResourceUsageReport;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.QueueState;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.util.Records;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TestTypeConverter {
  @Test
  public void testEnums() throws Exception {
    for (YarnApplicationState applicationState : YarnApplicationState.values()) {
      TypeConverter.fromYarn(applicationState, FinalApplicationStatus.FAILED);
    }
    // ad hoc test of NEW_SAVING, which is newly added
    assertEquals(State.PREP, TypeConverter.fromYarn(
        YarnApplicationState.NEW_SAVING, FinalApplicationStatus.FAILED));

    for (TaskType taskType : TaskType.values()) {
      TypeConverter.fromYarn(taskType);
    }

    for (JobState jobState : JobState.values()) {
      TypeConverter.fromYarn(jobState);
    }

    for (QueueState queueState : QueueState.values()) {
      TypeConverter.fromYarn(queueState);
    }

    for (TaskState taskState : TaskState.values()) {
      TypeConverter.fromYarn(taskState);
    }
  }

  @Test
  public void testFromYarn() throws Exception {
    int appStartTime = 612354;
    int appFinishTime = 612355;
    YarnApplicationState state = YarnApplicationState.RUNNING;
    ApplicationId applicationId = ApplicationId.newInstance(0, 0);
    ApplicationReport applicationReport = Records
        .newRecord(ApplicationReport.class);
    applicationReport.setApplicationId(applicationId);
    applicationReport.setYarnApplicationState(state);
    applicationReport.setStartTime(appStartTime);
    applicationReport.setFinishTime(appFinishTime);
    applicationReport.setUser("TestTypeConverter-user");
    applicationReport.setPriority(Priority.newInstance(3));
    ApplicationResourceUsageReport appUsageRpt = Records
        .newRecord(ApplicationResourceUsageReport.class);
    Resource r = Records.newRecord(Resource.class);
    r.setMemorySize(2048);
    appUsageRpt.setNeededResources(r);
    appUsageRpt.setNumReservedContainers(1);
    appUsageRpt.setNumUsedContainers(3);
    appUsageRpt.setReservedResources(r);
    appUsageRpt.setUsedResources(r);
    applicationReport.setApplicationResourceUsageReport(appUsageRpt);
    JobStatus jobStatus = TypeConverter.fromYarn(applicationReport, "dummy-jobfile");
    assertEquals(appStartTime, jobStatus.getStartTime());
    assertEquals(appFinishTime, jobStatus.getFinishTime());
    assertEquals(state.toString(), jobStatus.getState().toString());
    assertEquals(JobPriority.NORMAL, jobStatus.getPriority());
  }

  @Test
  public void testFromYarnApplicationReport() {
    ApplicationId mockAppId = mock(ApplicationId.class);
    when(mockAppId.getClusterTimestamp()).thenReturn(12345L);
    when(mockAppId.getId()).thenReturn(6789);

    ApplicationReport mockReport = mock(ApplicationReport.class);
    when(mockReport.getTrackingUrl()).thenReturn("dummy-tracking-url");
    when(mockReport.getApplicationId()).thenReturn(mockAppId);
    when(mockReport.getYarnApplicationState()).thenReturn(YarnApplicationState.KILLED);
    when(mockReport.getUser()).thenReturn("dummy-user");
    when(mockReport.getQueue()).thenReturn("dummy-queue");
    when(mockReport.getPriority()).thenReturn(Priority.newInstance(4));
    String jobFile = "dummy-path/job.xml";

    try {
      JobStatus status = TypeConverter.fromYarn(mockReport, jobFile);
    } catch (NullPointerException npe) {
      fail("Type converstion from YARN fails for jobs without " +
          "ApplicationUsageReport");
    }

    ApplicationResourceUsageReport appUsageRpt = Records
        .newRecord(ApplicationResourceUsageReport.class);
    Resource r = Records.newRecord(Resource.class);
    r.setMemorySize(2048);
    appUsageRpt.setNeededResources(r);
    appUsageRpt.setNumReservedContainers(1);
    appUsageRpt.setNumUsedContainers(3);
    appUsageRpt.setReservedResources(r);
    appUsageRpt.setUsedResources(r);
    when(mockReport.getApplicationResourceUsageReport()).thenReturn(appUsageRpt);
    JobStatus status = TypeConverter.fromYarn(mockReport, jobFile);
    assertNotNull(status, "fromYarn returned null status");
    assertEquals("dummy-path/job.xml", status.getJobFile(), "jobFile set incorrectly");
    assertEquals("dummy-queue", status.getQueue(), "queue set incorrectly");
    assertEquals("dummy-tracking-url", status.getTrackingUrl(), "trackingUrl set incorrectly");
    assertEquals("dummy-user", status.getUsername(), "user set incorrectly");
    assertEquals("dummy-tracking-url", status.getSchedulingInfo(),
        "schedulingInfo set incorrectly");
    assertEquals(6789, status.getJobID().getId(), "jobId set incorrectly");
    assertEquals(JobStatus.State.KILLED, status.getState(), "state set incorrectly");
    assertEquals(2048, status.getNeededMem(), "needed mem info set incorrectly");
    assertEquals(1, status.getNumReservedSlots(), "num rsvd slots info set incorrectly");
    assertEquals(3, status.getNumUsedSlots(), "num used slots info set incorrectly");
    assertEquals(2048, status.getReservedMem(), "rsvd mem info set incorrectly");
    assertEquals(2048, status.getUsedMem(), "used mem info set incorrectly");
    assertEquals(JobPriority.HIGH, status.getPriority(), "priority set incorrectly");
  }

  @Test
  public void testFromYarnQueueInfo() {
    org.apache.hadoop.yarn.api.records.QueueInfo queueInfo = Records
        .newRecord(org.apache.hadoop.yarn.api.records.QueueInfo.class);
    queueInfo.setQueueState(org.apache.hadoop.yarn.api.records.QueueState.STOPPED);
    org.apache.hadoop.mapreduce.QueueInfo returned =
        TypeConverter.fromYarn(queueInfo, new Configuration());
    assertEquals(returned.getState().toString(),
        StringUtils.toLowerCase(queueInfo.getQueueState().toString()),
        "queueInfo translation didn't work.");
  }

  /**
   * Test that child queues are converted too during conversion of the parent
   * queue
   */
  @Test
  public void testFromYarnQueue() {
    //Define child queue
    org.apache.hadoop.yarn.api.records.QueueInfo child =
        Mockito.mock(org.apache.hadoop.yarn.api.records.QueueInfo.class);
    Mockito.when(child.getQueueState()).thenReturn(QueueState.RUNNING);

    //Define parent queue
    org.apache.hadoop.yarn.api.records.QueueInfo queueInfo =
        Mockito.mock(org.apache.hadoop.yarn.api.records.QueueInfo.class);
    List<org.apache.hadoop.yarn.api.records.QueueInfo> children =
        new ArrayList<org.apache.hadoop.yarn.api.records.QueueInfo>();
    children.add(child); //Add one child
    Mockito.when(queueInfo.getChildQueues()).thenReturn(children);
    Mockito.when(queueInfo.getQueueState()).thenReturn(QueueState.RUNNING);

    //Call the function we're testing
    org.apache.hadoop.mapreduce.QueueInfo returned =
        TypeConverter.fromYarn(queueInfo, new Configuration());

    //Verify that the converted queue has the 1 child we had added
    assertThat(returned.getQueueChildren().size())
        .withFailMessage("QueueInfo children weren't properly converted")
        .isEqualTo(1);
  }

  @Test
  public void testFromYarnJobReport() throws Exception {
    int jobStartTime = 612354;
    int jobFinishTime = 612355;
    JobState state = JobState.RUNNING;
    JobId jobId = Records.newRecord(JobId.class);
    JobReport jobReport = Records.newRecord(JobReport.class);
    ApplicationId applicationId = ApplicationId.newInstance(0, 0);
    jobId.setAppId(applicationId);
    jobId.setId(0);
    jobReport.setJobId(jobId);
    jobReport.setJobState(state);
    jobReport.setStartTime(jobStartTime);
    jobReport.setFinishTime(jobFinishTime);
    jobReport.setUser("TestTypeConverter-user");
    jobReport.setJobPriority(Priority.newInstance(0));
    JobStatus jobStatus = TypeConverter.fromYarn(jobReport, "dummy-jobfile");
    assertEquals(jobStartTime, jobStatus.getStartTime());
    assertEquals(jobFinishTime, jobStatus.getFinishTime());
    assertEquals(state.toString(), jobStatus.getState().toString());
    assertEquals(JobPriority.DEFAULT, jobStatus.getPriority());
  }
}
