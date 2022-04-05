/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.model;

import java.util.Date;
import java.util.Objects;

public class RestIdentity
{
	private long entityId;
	private Date creationTs;
	private Date updateTs;
	private String comparableValue;

	public RestIdentity(long entityId, Date creationTs, Date updateTs, String comparableValue)
	{
		this.entityId = entityId;
		this.creationTs = creationTs;
		this.updateTs = updateTs;
		this.comparableValue = comparableValue;
	}

	//for Jackson
	protected RestIdentity()
	{
	}

	public long getEntityId()
	{
		return entityId;
	}

	public Date getCreationTs()
	{
		return creationTs;
	}

	public Date getUpdateTs()
	{
		return updateTs;
	}

	public String getComparableValue()
	{
		return comparableValue;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestIdentity that = (RestIdentity) o;
		return entityId == that.entityId && Objects.equals(creationTs, that.creationTs) && Objects.equals(updateTs, that.updateTs) && Objects.equals(comparableValue, that.comparableValue);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entityId, creationTs, updateTs, comparableValue);
	}

	@Override
	public String toString()
	{
		return "RestIdentity{" +
				"entityId=" + entityId +
				", creationTs=" + creationTs +
				", updateTs=" + updateTs +
				", comparableValue='" + comparableValue + '\'' +
				'}';
	}
}
