/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.audit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.NonNull;

import java.util.Objects;

/**
 * Holds information entity that initiate event or was subject of event.
 *
 * @author R. Ledzinski
 */
public class AuditEntity
{
	private final Long entityId;
	private final String name;
	private final String email;

	@JsonCreator
	public AuditEntity(@JsonProperty("entityId") @NonNull final Long entityId,
					   @JsonProperty("name") final String name,
					   @JsonProperty("email") final String email)
	{
		this.entityId = entityId;
		this.name = name;
		this.email = email;
	}

	public Long getEntityId()
	{
		return entityId;
	}

	public String getName()
	{
		return name;
	}

	public String getEmail()
	{
		return email;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AuditEntity that = (AuditEntity) o;
		return entityId.equals(that.entityId) &&
				Objects.equals(name, that.name) &&
				Objects.equals(email, that.email);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entityId);
	}

	@Override
	public String toString()
	{
		return "AuditEntity{" +
				"entityId=" + entityId +
				", name='" + name + '\'' +
				", email='" + email + '\'' +
				'}';
	}
}
