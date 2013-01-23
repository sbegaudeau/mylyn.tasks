/*******************************************************************************
 * Copyright (c) 2012 Frank Becker and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.bugzilla.tests.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.bugzilla.tests.AbstractBugzillaTest;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaAttribute;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaTaskDataHandler;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.data.FileTaskAttachmentSource;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.RepositoryResponse.ResponseKind;
import org.eclipse.mylyn.tasks.core.TaskMapping;
import org.eclipse.mylyn.tasks.core.data.ITaskDataWorkingCopy;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;

public class BugzillaHarness {

	AbstractBugzillaTest abstractBugzillaTest;

	public BugzillaHarness(AbstractBugzillaTest abstractBugzillaTest) {
		this.abstractBugzillaTest = abstractBugzillaTest;
	}

	public String taskXmlRpcExists() {
		String taskID = null;
		String queryUrlString = abstractBugzillaTest.getRepository().getRepositoryUrl() + "/buglist.cgi?"
				+ "short_desc=test%20XMLRPC%20getBugData&resolution=---&query_format=advanced"
				+ "&short_desc_type=casesubstring&component=TestComponent&product=TestProduct";
		RepositoryQuery query = new RepositoryQuery(abstractBugzillaTest.getRepository().getConnectorKind(),
				"handle-testQueryViaConnector");
		query.setUrl(queryUrlString);
		final Map<Integer, TaskData> changedTaskData = new HashMap<Integer, TaskData>();
		TaskDataCollector collector = new TaskDataCollector() {
			@Override
			public void accept(TaskData taskData) {
				changedTaskData.put(Integer.valueOf(taskData.getTaskId()), taskData);
			}
		};
		abstractBugzillaTest.getConnector().performQuery(abstractBugzillaTest.getRepository(), query, collector, null,
				new NullProgressMonitor());
		if (changedTaskData.size() > 0) {
			Set<Integer> ks = changedTaskData.keySet();
			SortedSet<Integer> sks = new TreeSet<Integer>(ks);
			taskID = sks.last().toString();
		}
		return taskID;
	}

	public String createXmlRpcTask() throws Exception {
		final TaskMapping taskMappingInit = new TaskMapping() {

			@Override
			public String getProduct() {
				return "TestProduct";
			}
		};
		final TaskMapping taskMappingSelect = new TaskMapping() {
			@Override
			public String getComponent() {
				return "TestComponent";
			}

			@Override
			public String getSummary() {
				return "test XMLRPC getBugData";
			}

			@Override
			public String getDescription() {
				return "The Description of the XMLRPC getBugData Bug";
			}
		};
		TaskAttribute flagA = null;
		TaskAttribute flagB = null;
		TaskAttribute flagC = null;
		TaskAttribute flagD = null;
		TaskAttribute stateA = null;
		TaskAttribute stateB = null;
		TaskAttribute stateC = null;
		TaskAttribute stateD = null;
		final TaskData[] taskDataNew = new TaskData[1];

		// create Task
		taskDataNew[0] = TasksUiInternal.createTaskData(abstractBugzillaTest.getRepository(), taskMappingInit,
				taskMappingSelect, null);
		ITask taskNew = TasksUiUtil.createOutgoingNewTask(taskDataNew[0].getConnectorKind(),
				taskDataNew[0].getRepositoryUrl());

		ITaskDataWorkingCopy workingCopy = TasksUi.getTaskDataManager().createWorkingCopy(taskNew, taskDataNew[0]);
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		workingCopy.save(changed, null);

		RepositoryResponse response = BugzillaFixture.current().submitTask(taskDataNew[0],
				abstractBugzillaTest.getClient());//connector.getTaskDataHandler().postTaskData(repository, taskDataNew[0], changed,

		((AbstractTask) taskNew).setSubmitting(true);

		assertNotNull(response);
		assertEquals(ResponseKind.TASK_CREATED.toString(), response.getReposonseKind().toString());
		String taskId = response.getTaskId();

		ITask task = abstractBugzillaTest.generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		TaskDataModel model = abstractBugzillaTest.createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);
		TaskAttribute attrAttachment = taskData.getAttributeMapper().createTaskAttachment(taskData);
		TaskAttachmentMapper attachmentMapper = TaskAttachmentMapper.createFrom(attrAttachment);

		/* Test uploading a proper file */
		String fileName = "test-attach-1.txt";
		File attachFile = new File(fileName);
		attachFile.createNewFile();
		attachFile.deleteOnExit();
		BufferedWriter write = new BufferedWriter(new FileWriter(attachFile));
		write.write("test file from " + System.currentTimeMillis());
		write.close();

		FileTaskAttachmentSource attachment = new FileTaskAttachmentSource(attachFile);
		attachment.setContentType("text/plain");
		attachment.setDescription("Description");
		attachment.setName("My Attachment 1");
		try {
			abstractBugzillaTest.getClient().postAttachment(taskData.getTaskId(), attachmentMapper.getComment(),
					attachment, attrAttachment, new NullProgressMonitor());
		} catch (Exception e) {
			fail("never reach this!");
		}
		taskData = BugzillaFixture.current().getTask(taskData.getTaskId(), abstractBugzillaTest.getClient());
		assertNotNull(taskData);

		TaskAttribute attachmentAttribute = taskData.getAttributeMapper()
				.getAttributesByType(taskData, TaskAttribute.TYPE_ATTACHMENT)
				.get(0);
		int flagCount = 0;
		int flagCountUnused = 0;
		TaskAttribute attachmentFlag1 = null;
		TaskAttribute attachmentFlag2 = null;
		for (TaskAttribute attribute : attachmentAttribute.getAttributes().values()) {
			if (!attribute.getId().startsWith(BugzillaAttribute.KIND_FLAG)) {
				continue;
			}
			flagCount++;
			if (attribute.getId().startsWith(BugzillaAttribute.KIND_FLAG_TYPE)) {
				flagCountUnused++;
				TaskAttribute stateAttribute = taskData.getAttributeMapper().getAssoctiatedAttribute(attribute);
				if (stateAttribute.getMetaData().getLabel().equals("AttachmentFlag1")) {
					attachmentFlag1 = attribute;
				}
				if (stateAttribute.getMetaData().getLabel().equals("AttachmentFlag2")) {
					attachmentFlag2 = attribute;
				}
			}
		}
		assertEquals(2, flagCount);
		assertEquals(2, flagCountUnused);
		assertNotNull(attachmentFlag1);
		assertNotNull(attachmentFlag2);
		TaskAttribute stateAttribute1 = taskData.getAttributeMapper().getAssoctiatedAttribute(attachmentFlag1);
		stateAttribute1.setValue("?");
		TaskAttribute requestee = attachmentFlag1.getAttribute("requestee"); //$NON-NLS-1$
		requestee.setValue("guest@mylyn.eclipse.org");
		abstractBugzillaTest.getClient().postUpdateAttachment(attachmentAttribute, "update", null);

		task = abstractBugzillaTest.generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		model = abstractBugzillaTest.createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);

		for (TaskAttribute taskAttribute : taskData.getRoot().getAttributes().values()) {
			if (taskAttribute.getId().startsWith(BugzillaAttribute.KIND_FLAG)) {
				TaskAttribute state = taskAttribute.getAttribute("state");
				if (state.getMetaData().getLabel().equals("BugFlag1")) {
					flagA = taskAttribute;
					stateA = state;
				} else if (state.getMetaData().getLabel().equals("BugFlag2")) {
					flagB = taskAttribute;
					stateB = state;
				} else if (state.getMetaData().getLabel().equals("BugFlag3")) {
					flagC = taskAttribute;
					stateC = state;
				} else if (state.getMetaData().getLabel().equals("BugFlag4")) {
					flagD = taskAttribute;
					stateD = state;
				}
			}
		}
		assertNotNull(flagA);
		assertNotNull(flagB);
		assertNotNull(flagC);
		assertNotNull(flagD);
		assertNotNull(stateA);
		assertNotNull(stateB);
		assertNotNull(stateC);
		assertNotNull(stateD);
		if (flagD != null) {
			TaskAttribute requesteeD = flagD.getAttribute("requestee");
			requesteeD.setValue("guest@mylyn.eclipse.org");
		}
		if (stateA != null) {
			stateA.setValue("+");
		}
		if (stateB != null) {
			stateB.setValue("?");
		}
		if (stateC != null) {
			stateC.setValue("?");
		}
		if (stateD != null) {
			stateD.setValue("?");
		}

		TaskAttribute cf_freetext = taskData.getRoot().getAttribute("cf_freetext");
		TaskAttribute cf_dropdown = taskData.getRoot().getAttribute("cf_dropdown");
		TaskAttribute cf_largetextbox = taskData.getRoot().getAttribute("cf_largetextbox");
		TaskAttribute cf_multiselect = taskData.getRoot().getAttribute("cf_multiselect");
		TaskAttribute cf_datetime = taskData.getRoot().getAttribute("cf_datetime");
		TaskAttribute cf_bugid = taskData.getRoot().getAttribute("cf_bugid");
		cf_freetext.setValue("Freetext");
		cf_dropdown.setValue("one");
		cf_largetextbox.setValue("large text box");
		cf_multiselect.setValue("Blue");
		cf_datetime.setValue("2012-01-01 00:00:00");
		cf_bugid.setValue("3");

		model.attributeChanged(cf_freetext);
		model.attributeChanged(cf_dropdown);
		model.attributeChanged(cf_largetextbox);
		model.attributeChanged(cf_multiselect);
		model.attributeChanged(cf_datetime);
		model.attributeChanged(cf_bugid);
		model.attributeChanged(flagA);
		model.attributeChanged(flagB);
		model.attributeChanged(flagC);
		model.attributeChanged(flagD);
		changed.clear();
		changed.add(flagA);
		changed.add(flagB);
		changed.add(flagC);
		changed.add(flagD);
		changed.add(cf_freetext);
		changed.add(cf_dropdown);
		changed.add(cf_largetextbox);
		changed.add(cf_multiselect);
		changed.add(cf_datetime);
		changed.add(cf_bugid);

		workingCopy.save(changed, null);
		response = BugzillaFixture.current().submitTask(taskData, abstractBugzillaTest.getClient());

		return taskId;
	}

	public String taskAttachmentAttributesExists() {
		String taskID = null;
		String queryUrlString = abstractBugzillaTest.getRepository().getRepositoryUrl() + "/buglist.cgi?"
				+ "short_desc=test%20Bug%20with%20Attachment&resolution=---&query_format=advanced"
				+ "&short_desc_type=casesubstring&component=ManualC2&product=ManualTest";
		RepositoryQuery query = new RepositoryQuery(abstractBugzillaTest.getRepository().getConnectorKind(),
				"handle-testQueryViaConnector");
		query.setUrl(queryUrlString);
		final Map<Integer, TaskData> changedTaskData = new HashMap<Integer, TaskData>();
		TaskDataCollector collector = new TaskDataCollector() {
			@Override
			public void accept(TaskData taskData) {
				changedTaskData.put(Integer.valueOf(taskData.getTaskId()), taskData);
			}
		};
		abstractBugzillaTest.getConnector().performQuery(abstractBugzillaTest.getRepository(), query, collector, null,
				new NullProgressMonitor());
		if (changedTaskData.size() > 0) {
			Set<Integer> ks = changedTaskData.keySet();
			SortedSet<Integer> sks = new TreeSet<Integer>(ks);
			taskID = sks.last().toString();
		}
		return taskID;
	}

	public String createAttachmentAttributesTask() throws Exception {
		final TaskMapping taskMappingInit = new TaskMapping() {

			@Override
			public String getProduct() {
				return "ManualTest";
			}
		};
		final TaskMapping taskMappingSelect = new TaskMapping() {
			@Override
			public String getComponent() {
				return "ManualC2";
			}

			@Override
			public String getSummary() {
				return "test Bug with Attachment";
			}

			@Override
			public String getDescription() {
				return "The Description of the test with Attachment Bug";
			}
		};
		final TaskData[] taskDataNew = new TaskData[1];

		// create Task
		taskDataNew[0] = TasksUiInternal.createTaskData(abstractBugzillaTest.getRepository(), taskMappingInit,
				taskMappingSelect, null);
		ITask taskNew = TasksUiUtil.createOutgoingNewTask(taskDataNew[0].getConnectorKind(),
				taskDataNew[0].getRepositoryUrl());

		ITaskDataWorkingCopy workingCopy = TasksUi.getTaskDataManager().createWorkingCopy(taskNew, taskDataNew[0]);
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		workingCopy.save(changed, null);

		RepositoryResponse response = BugzillaFixture.current().submitTask(taskDataNew[0],
				abstractBugzillaTest.getClient());//connector.getTaskDataHandler().postTaskData(repository, taskDataNew[0], changed,
		//new NullProgressMonitor());
		((AbstractTask) taskNew).setSubmitting(true);

		assertNotNull(response);
		assertEquals(ResponseKind.TASK_CREATED.toString(), response.getReposonseKind().toString());
		String taskId = response.getTaskId();

		ITask task = abstractBugzillaTest.generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		TaskDataModel model = abstractBugzillaTest.createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);
		TaskAttribute attrAttachment = taskData.getAttributeMapper().createTaskAttachment(taskData);
		TaskAttachmentMapper attachmentMapper = TaskAttachmentMapper.createFrom(attrAttachment);

		String fileName = "test-attach-1.txt";
		File attachFile = new File(fileName);
		attachFile.createNewFile();
		attachFile.deleteOnExit();
		BufferedWriter write = new BufferedWriter(new FileWriter(attachFile));
		write.write("test file from " + System.currentTimeMillis());
		write.close();

		FileTaskAttachmentSource attachment = new FileTaskAttachmentSource(attachFile);
		attachment.setContentType("text/plain");
		attachment.setDescription("My Attachment 1");
		attachment.setName("My Attachment 1");
		try {
			abstractBugzillaTest.getClient().postAttachment(taskData.getTaskId(), attachmentMapper.getComment(),
					attachment, attrAttachment, new NullProgressMonitor());
		} catch (Exception e) {
			fail("never reach this!");
		}
		taskData = BugzillaFixture.current().getTask(taskData.getTaskId(), abstractBugzillaTest.getClient());
		assertNotNull(taskData);

		task = abstractBugzillaTest.generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		model = abstractBugzillaTest.createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		attrAttachment = taskData.getAttributeMapper().createTaskAttachment(taskData);
		attachmentMapper = TaskAttachmentMapper.createFrom(attrAttachment);

		fileName = "test-attach-2.txt";
		attachFile = new File(fileName);
		attachFile.createNewFile();
		attachFile.deleteOnExit();
		write = new BufferedWriter(new FileWriter(attachFile));
		write.write("test file 2 from " + System.currentTimeMillis());
		write.close();

		attachment = new FileTaskAttachmentSource(attachFile);
		attachment.setContentType("text/plain");
		attachment.setDescription("My Attachment 2");
		attachment.setName("My Attachment 2");
		try {
			abstractBugzillaTest.getClient().postAttachment(taskData.getTaskId(), attachmentMapper.getComment(),
					attachment, attrAttachment, new NullProgressMonitor());
		} catch (Exception e) {
			fail("never reach this!");
		}
		taskData = BugzillaFixture.current().getTask(taskData.getTaskId(), abstractBugzillaTest.getClient());
		assertNotNull(taskData);

		task = abstractBugzillaTest.generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		model = abstractBugzillaTest.createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		attrAttachment = taskData.getAttributeMapper().createTaskAttachment(taskData);
		attachmentMapper = TaskAttachmentMapper.createFrom(attrAttachment);

		fileName = "test-attach-3.txt";
		attachFile = new File(fileName);
		attachFile.createNewFile();
		attachFile.deleteOnExit();
		write = new BufferedWriter(new FileWriter(attachFile));
		write.write("test file 3 from " + System.currentTimeMillis());
		write.close();

		attachment = new FileTaskAttachmentSource(attachFile);
		attachment.setContentType("text/plain");
		attachment.setDescription("My Attachment 3");
		attachment.setName("My Attachment 3");
		TaskAttribute child = attrAttachment.createMappedAttribute(TaskAttribute.ATTACHMENT_IS_PATCH);
		if (child != null) {
			child.setValue("1");
		}
		try {
			abstractBugzillaTest.getClient().postAttachment(taskData.getTaskId(), attachmentMapper.getComment(),
					attachment, attrAttachment, new NullProgressMonitor());
		} catch (Exception e) {
			fail("never reach this!");
		}
		taskData = BugzillaFixture.current().getTask(taskData.getTaskId(), abstractBugzillaTest.getClient());
		assertNotNull(taskData);

		task = abstractBugzillaTest.generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		model = abstractBugzillaTest.createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);
		attrAttachment = taskData.getAttributeMapper().createTaskAttachment(taskData);
		attachmentMapper = TaskAttachmentMapper.createFrom(attrAttachment);

		fileName = "test-attach-4.txt";
		attachFile = new File(fileName);
		attachFile.createNewFile();
		attachFile.deleteOnExit();
		write = new BufferedWriter(new FileWriter(attachFile));
		write.write("test file 4 from " + System.currentTimeMillis());
		write.close();

		attachment = new FileTaskAttachmentSource(attachFile);
		attachment.setContentType("text/plain");
		attachment.setDescription("My Attachment 4");
		attachment.setName("My Attachment 4");
		child = attrAttachment.createMappedAttribute(TaskAttribute.ATTACHMENT_IS_PATCH);
		if (child != null) {
			child.setValue("1");
		}
		try {
			abstractBugzillaTest.getClient().postAttachment(taskData.getTaskId(), attachmentMapper.getComment(),
					attachment, attrAttachment, new NullProgressMonitor());
		} catch (Exception e) {
			fail("never reach this!");
		}
		taskData = BugzillaFixture.current().getTask(taskData.getTaskId(), abstractBugzillaTest.getClient());
		assertNotNull(taskData);

		TaskAttribute attachment1 = taskData.getAttributeMapper()
				.getAttributesByType(taskData, TaskAttribute.TYPE_ATTACHMENT)
				.get(1);
		assertNotNull(attachment1);
		TaskAttribute obsolete = attachment1.getMappedAttribute(TaskAttribute.ATTACHMENT_IS_DEPRECATED);
		assertNotNull(obsolete);
		obsolete.setValue("1"); //$NON-NLS-1$
		((BugzillaTaskDataHandler) abstractBugzillaTest.getConnector().getTaskDataHandler()).postUpdateAttachment(
				abstractBugzillaTest.getRepository(), attachment1, "update", new NullProgressMonitor()); //$NON-NLS-1$

		task = abstractBugzillaTest.generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		model = abstractBugzillaTest.createModel(task);
		taskData = model.getTaskData();
		assertNotNull(taskData);

		TaskAttribute attachment2 = taskData.getAttributeMapper()
				.getAttributesByType(taskData, TaskAttribute.TYPE_ATTACHMENT)
				.get(3);
		assertNotNull(attachment);
		obsolete = attachment2.getMappedAttribute(TaskAttribute.ATTACHMENT_IS_DEPRECATED);
		assertNotNull(obsolete);
		obsolete.setValue("1"); //$NON-NLS-1$
		((BugzillaTaskDataHandler) abstractBugzillaTest.getConnector().getTaskDataHandler()).postUpdateAttachment(
				abstractBugzillaTest.getRepository(), attachment2, "update", new NullProgressMonitor()); //$NON-NLS-1$

		return taskId;
	}

	public String taskCustomFieldExists() {
		String taskID = null;
		String queryUrlString = abstractBugzillaTest.getRepository().getRepositoryUrl() + "/buglist.cgi?"
				+ "short_desc=test%20Bug%20with%20Custom%20Fields&resolution=---&query_format=advanced"
				+ "&short_desc_type=casesubstring&component=ManualC2&product=ManualTest";
		RepositoryQuery query = new RepositoryQuery(abstractBugzillaTest.getRepository().getConnectorKind(),
				"handle-testQueryViaConnector");
		query.setUrl(queryUrlString);
		final Map<Integer, TaskData> changedTaskData = new HashMap<Integer, TaskData>();
		TaskDataCollector collector = new TaskDataCollector() {
			@Override
			public void accept(TaskData taskData) {
				changedTaskData.put(Integer.valueOf(taskData.getTaskId()), taskData);
			}
		};
		abstractBugzillaTest.getConnector().performQuery(abstractBugzillaTest.getRepository(), query, collector, null,
				new NullProgressMonitor());
		if (changedTaskData.size() > 0) {
			Set<Integer> ks = changedTaskData.keySet();
			SortedSet<Integer> sks = new TreeSet<Integer>(ks);
			taskID = sks.last().toString();
		}
		return taskID;
	}

	public String createCustomFieldTask() throws Exception {
		final TaskMapping taskMappingInit = new TaskMapping() {

			@Override
			public String getProduct() {
				return "ManualTest";
			}
		};
		final TaskMapping taskMappingSelect = new TaskMapping() {
			@Override
			public String getComponent() {
				return "ManualC2";
			}

			@Override
			public String getSummary() {
				return "test Bug with Custom Fields";
			}

			@Override
			public String getDescription() {
				return "The Description of the test with Custom Fields Bug";
			}
		};
		final TaskData[] taskDataNew = new TaskData[1];

		// create Task
		taskDataNew[0] = TasksUiInternal.createTaskData(abstractBugzillaTest.getRepository(), taskMappingInit,
				taskMappingSelect, null);
		ITask taskNew = TasksUiUtil.createOutgoingNewTask(taskDataNew[0].getConnectorKind(),
				taskDataNew[0].getRepositoryUrl());

		ITaskDataWorkingCopy workingCopy = TasksUi.getTaskDataManager().createWorkingCopy(taskNew, taskDataNew[0]);
		Set<TaskAttribute> changed = new HashSet<TaskAttribute>();
		workingCopy.save(changed, null);

		RepositoryResponse response = BugzillaFixture.current().submitTask(taskDataNew[0],
				abstractBugzillaTest.getClient());//connector.getTaskDataHandler().postTaskData(repository, taskDataNew[0], changed,
		//new NullProgressMonitor());
		((AbstractTask) taskNew).setSubmitting(true);

		assertNotNull(response);
		assertEquals(ResponseKind.TASK_CREATED.toString(), response.getReposonseKind().toString());
		String taskId = response.getTaskId();

		ITask task = abstractBugzillaTest.generateLocalTaskAndDownload(taskId);
		assertNotNull(task);
		TaskDataModel model = abstractBugzillaTest.createModel(task);
		TaskData taskData = model.getTaskData();
		assertNotNull(taskData);

		TaskMapper mapper = new TaskMapper(taskData);
		TaskAttribute cf_multiselect = mapper.getTaskData().getRoot().getAttribute("cf_multiselect");
		cf_multiselect.setValue("Green");
		model.attributeChanged(cf_multiselect);
		changed.clear();
		changed.add(cf_multiselect);

		workingCopy.save(changed, null);
		response = BugzillaFixture.current().submitTask(taskData, abstractBugzillaTest.getClient());

		return taskId;
	}

}
