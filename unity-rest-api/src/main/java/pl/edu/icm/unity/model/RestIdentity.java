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
	private String typeId;
	protected String value;
	protected String target;
	protected String realm;

	public RestIdentity(long entityId, Date creationTs, Date updateTs, String comparableValue, String typeId, String value, String target, String realm)
	{
		this.entityId = entityId;
		this.creationTs = creationTs;
		this.updateTs = updateTs;
		this.comparableValue = comparableValue;
		this.typeId = typeId;
		this.value = value;
		this.target = target;
		this.realm = realm;
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

	public String getTypeId()
	{
		return typeId;
	}

	public String getValue()
	{
		return value;
	}

	public String getTarget()
	{
		return target;
	}

	public String getRealm()
	{
		return realm;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestIdentity that = (RestIdentity) o;
		return entityId == that.entityId && Objects.equals(creationTs, that.creationTs) && Objects.equals(updateTs, that.updateTs) && Objects.equals(comparableValue, that.comparableValue) && Objects.equals(typeId, that.typeId) && Objects.equals(value, that.value) && Objects.equals(target, that.target) && Objects.equals(realm, that.realm);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entityId, creationTs, updateTs, comparableValue, typeId, value, target, realm);
	}

	@Override
	public String toString()
	{
		return "RestIdentity{" +
				"entityId=" + entityId +
				", creationTs=" + creationTs +
				", updateTs=" + updateTs +
				", comparableValue='" + comparableValue + '\'' +
				", typeId='" + typeId + '\'' +
				", value='" + value + '\'' +
				", target='" + target + '\'' +
				", realm='" + realm + '\'' +
				'}';
	}
}
