/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.internal.tasks.core.sync;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.AbstractTaskDataHandler2;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.ITaskDataManager;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.sync.SubmitJob;

/**
 * @author Steffen Pingel
 */
public class SubmitTaskJob extends SubmitJob {

	private static final String LABEL_JOB_SUBMIT = "Submitting to repository";

	private final TaskRepository taskRepository;

	private final TaskData taskData;

	private final AbstractRepositoryConnector connector;

	private IStatus errorStatus;

	private AbstractTask task;

	private final Set<TaskAttribute> changedAttributes;

	private final ITaskDataManager taskDataManager;

	private RepositoryResponse response;

	public SubmitTaskJob(ITaskDataManager taskDataManager, AbstractRepositoryConnector connector,
			TaskRepository taskRepository, AbstractTask task, TaskData taskData, Set<TaskAttribute> changedAttributes) {
		super(LABEL_JOB_SUBMIT);
		this.taskDataManager = taskDataManager;
		this.connector = connector;
		this.taskRepository = taskRepository;
		this.task = task;
		this.taskData = taskData;
		this.changedAttributes = changedAttributes;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			monitor.beginTask("Submitting task", 2 * (1 + getSubmitJobListeners().length) * 100);

			// post task data
			AbstractTaskDataHandler2 taskDataHandler = connector.getTaskDataHandler2();
			monitor.subTask("Sending data");
			response = taskDataHandler.postTaskData(taskRepository, taskData, changedAttributes, Policy.subMonitorFor(
					monitor, 100));
			if (response == null || response.getTaskId() == null) {
				throw new CoreException(new RepositoryStatus(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN,
						RepositoryStatus.ERROR_INTERNAL,
						"Task could not be created. No additional information was provided by the connector."));
			}
			fireTaskDataPosted(monitor);

			// update task in task list
			String taskId = response.getTaskId();
			monitor.subTask("Receiving data");
			TaskData updatedTaskData = connector.getTaskData2(taskRepository, taskId,
					Policy.subMonitorFor(monitor, 100));
			task = createTask(monitor, updatedTaskData);
			taskDataManager.putSubmittedTaskData(task, updatedTaskData);
			fireTaskSynchronized(monitor);
		} catch (CoreException e) {
			errorStatus = e.getStatus();
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		} catch (Exception e) {
			errorStatus = new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN, e.getMessage(), e);
		} finally {
			monitor.done();
		}
		fireDone();
		return Status.OK_STATUS;
	}

	private AbstractTask createTask(IProgressMonitor monitor, TaskData updatedTaskData) throws CoreException {
		if (taskData.isNew()) {
			task = connector.createTask(taskRepository.getRepositoryUrl(), updatedTaskData.getTaskId(), "");
		}
		return task;
	}

	@Override
	public IStatus getError() {
		return errorStatus;
	}

	public AbstractTask getTask() {
		return task;
	}

}