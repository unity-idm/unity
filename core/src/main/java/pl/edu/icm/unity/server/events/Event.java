/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.events;

import java.util.Date;

/**
 * Describes execution of operation with its context. Suitable for auditing, 
 * time measurement, logging or sending notifications. 
 * @author K. Benedyczak
 */
public class Event
{
	private String category;
	private Long invokerEntity;
	private Date timestamp;
	private String contents;
	
	public Event(String category, Long invokerEntity, Date timestamp)
	{
		this(category, invokerEntity, timestamp, null);
	}

	public Event(String category, Long invokerEntity, Date timestamp, String contents)
	{
		this.category = category;
		this.invokerEntity = invokerEntity;
		this.timestamp = timestamp;
		this.contents = contents;
	}

	public String getCategory()
	{
		return category;
	}

	public void setCategory(String category)
	{
		this.category = category;
	}

	public Long getInvokerEntity()
	{
		return invokerEntity;
	}

	public void setInvokerEntity(Long invokerEntity)
	{
		this.invokerEntity = invokerEntity;
	}

	public Date getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
	}

	public String getContents()
	{
		return contents;
	}

	public void setContents(String contents)
	{
		this.contents = contents;
	}

	@Override
	public String toString()
	{
		return "[category=" + category + ", invokerEntity=" + invokerEntity
				+ ", timestamp=" + timestamp + ", contents=" + contents + "]";
	}
}
