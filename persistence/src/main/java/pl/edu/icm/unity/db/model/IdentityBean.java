/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;


/**
 * In DB identity representation.
 * @author K. Benedyczak
 */
public class IdentityBean extends BaseBean
{
	private Long entityId;
	private Long typeId;
	
	public IdentityBean() 
	{
	}
	
	public Long getEntityId()
	{
		return entityId;
	}
	public void setEntityId(Long entityId)
	{
		this.entityId = entityId;
	}

	public Long getTypeId()
	{
		return typeId;
	}

	public void setTypeId(Long typeId)
	{
		this.typeId = typeId;
	}
}
