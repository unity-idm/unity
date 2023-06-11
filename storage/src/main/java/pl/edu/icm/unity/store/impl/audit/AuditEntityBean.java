/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import pl.edu.icm.unity.base.audit.AuditEntity;

/**
 * In DB audit event representation.
 *
 * @author R. Ledzinski
 */
class AuditEntityBean
{
	private Long id;
	private Long entityId;
	private String name;
	private String email;

	public AuditEntityBean()
	{
	}

	public AuditEntityBean(final AuditEntity auditEntity)
	{
		this.entityId = auditEntity.getEntityId();
		this.name = auditEntity.getName();
		this.email = auditEntity.getEmail();
	}

	public Long getId()
	{
		return id;
	}

	public void setId(final Long id)
	{
		this.id = id;
	}

	public Long getEntityId()
	{
		return entityId;
	}

	public void setEntityId(final Long entityId)
	{
		this.entityId = entityId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(final String name)
	{
		this.name = name;
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail(final String email)
	{
		this.email = email;
	}
}
