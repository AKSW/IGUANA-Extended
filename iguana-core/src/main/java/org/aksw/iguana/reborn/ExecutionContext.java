package org.aksw.iguana.reborn;

import java.util.Calendar;

import org.apache.jena.rdf.model.Resource;

public class ExecutionContext<T> {
	protected Resource taskResource;
	protected T taskEntity;
	protected Calendar startTime;

	public ExecutionContext(Resource taskResource, T taskEntity, Calendar startTime) {
		super();
		this.taskResource = taskResource;
		this.taskEntity = taskEntity;
		this.startTime = startTime;
	}

	public Resource getTaskResource() {
		return taskResource;
	}

	public T getTaskEntity() {
		return taskEntity;
	}

	public Calendar getStartTime() {
		return startTime;
	}

	@Override
	public String toString() {
		return "ExecutionContext [taskResource=" + taskResource + ", taskEntity=" + taskEntity + ", startTime="
				+ startTime + "]";
	}
}
