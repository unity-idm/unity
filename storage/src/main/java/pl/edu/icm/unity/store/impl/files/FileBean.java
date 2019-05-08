/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.store.impl.files;

import java.util.Date;

import pl.edu.icm.unity.store.rdbms.BaseBean;

/**
 * 
 * @author P.Piernik
 *
 */
public class FileBean  extends BaseBean
{
	private String ownerType;
	private String ownerId;
	private Date lastUpdate;
	
	public FileBean()
	{
		super();
	}
	public FileBean(String name, String ownerType, String ownerId)
	{
		super(name, null);
		this.setOwnerType(ownerType);
		this.setOwnerId(ownerId);
	}
	
	public FileBean(String name, String ownerType, String ownerId, byte[] contents)
	{
		super(name, contents);
		this.setOwnerType(ownerType);
		this.setOwnerId(ownerId);
	}
	
	public String getOwnerType()
	{
		return ownerType;
	}
	public void setOwnerType(String ownerType)
	{
		this.ownerType = ownerType;
	}
	public String getOwnerId()
	{
		return ownerId;
	}
	public void setOwnerId(String ownerId)
	{
		this.ownerId = ownerId;
	}
	public Date getLastUpdate()
	{
		return lastUpdate;
	}
	public void setLastUpdate(Date lastUpdate)
	{
		this.lastUpdate = lastUpdate;
	}
}
