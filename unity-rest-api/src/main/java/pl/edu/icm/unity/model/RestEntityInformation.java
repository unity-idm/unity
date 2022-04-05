/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.model;

import java.util.Date;
import java.util.Objects;

public class RestEntityInformation
{
	private Long id;
	private String entityState;
	private Date scheduledOperationTime;
	private String scheduledOperation;
	private Date removalByUserTime;

	public RestEntityInformation(Long id, String entityState, Date scheduledOperationTime, String scheduledOperation, Date removalByUserTime)
	{
		this.id = id;
		this.entityState = entityState;
		this.scheduledOperationTime = scheduledOperationTime;
		this.scheduledOperation = scheduledOperation;
		this.removalByUserTime = removalByUserTime;
	}

	//for Jackson
	protected RestEntityInformation()
	{
	}

	public Long getId()
	{
		return id;
	}

	public String getEntityState()
	{
		return entityState;
	}

	public Date getScheduledOperationTime()
	{
		return scheduledOperationTime;
	}

	public String getScheduledOperation()
	{
		return scheduledOperation;
	}

	public Date getRemovalByUserTime()
	{
		return removalByUserTime;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestEntityInformation that = (RestEntityInformation) o;
		return Objects.equals(id, that.id) && Objects.equals(entityState, that.entityState) && Objects.equals(scheduledOperationTime, that.scheduledOperationTime) && Objects.equals(scheduledOperation, that.scheduledOperation) && Objects.equals(removalByUserTime, that.removalByUserTime);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, entityState, scheduledOperationTime, scheduledOperation, removalByUserTime);
	}

	@Override
	public String toString()
	{
		return "RestEntityInformation{" +
				"id=" + id +
				", entityState='" + entityState + '\'' +
				", scheduledOperationTime=" + scheduledOperationTime +
				", scheduledOperation='" + scheduledOperation + '\'' +
				", removalByUserTime=" + removalByUserTime +
				'}';
	}
}
