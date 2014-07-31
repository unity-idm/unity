/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.ArrayList;
import java.util.Date;

/**
 * Extends the basic {@link Attribute} with metadata which is set by the server when returning 
 * attributes.
 * @author K. Benedyczak
 * @param <T>
 */
public class AttributeExt<T> extends Attribute<T>
{
	private boolean direct;
	private Date creationTs;
	private Date updateTs;
	
	public AttributeExt(Attribute<T> baseAttribute, boolean isDirect, Date creationTs, Date updateTs)
	{
		super(baseAttribute.getName(), baseAttribute.getAttributeSyntax(), baseAttribute.getGroupPath(), 
				baseAttribute.getVisibility(), new ArrayList<T>(baseAttribute.getValues()),
				baseAttribute.getRemoteIdp(), baseAttribute.getTranslationProfile());
		this.direct = isDirect;
		this.creationTs = creationTs;
		this.updateTs = updateTs;
	}
	
	/**
	 * Cloning constructor. Deep cloning is performed.
	 * @param source
	 */
	public AttributeExt(AttributeExt<T> source, Date creationTs, Date updateTs)
	{
		this(source, source.isDirect(), creationTs, updateTs);
	}

	public AttributeExt(Attribute<T> baseAttribute, boolean isDirect)
	{
		this(baseAttribute, isDirect, null, null);
	}
	
	/**
	 * Cloning constructor. Deep cloning is performed.
	 * @param source
	 */
	public AttributeExt(AttributeExt<T> source)
	{
		this(source, source.isDirect(), source.creationTs, source.updateTs);
	}
	
	public AttributeExt()
	{
	}
	
	/**
	 * @return if true, the attribute is direclt defined in the group of its scope. If false it is an 
	 * implied attribute, assigned by group's attribute statements.
	 */
	public boolean isDirect()
	{
		return direct;
	}

	public void setDirect(boolean direct)
	{
		this.direct = direct;
	}

	public Date getCreationTs()
	{
		return creationTs;
	}

	public void setCreationTs(Date creationTs)
	{
		this.creationTs = creationTs;
	}

	public Date getUpdateTs()
	{
		return updateTs;
	}

	public void setUpdateTs(Date updateTs)
	{
		this.updateTs = updateTs;
	}
}
