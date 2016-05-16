/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic2;

import java.util.ArrayList;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.types.basic.AttributeExt;

/**
 * Extends the basic {@link Attribute2} with metadata which is set by the server when returning 
 * attributes. Will replace {@link AttributeExt}.
 * @author K. Benedyczak
 */
public class AttributeExt2 extends Attribute2
{
	private boolean direct;
	private Date creationTs;
	private Date updateTs;
	
	public AttributeExt2(Attribute2 baseAttribute, boolean isDirect, Date creationTs, Date updateTs)
	{
		super(baseAttribute.getName(), baseAttribute.getValueSyntax(), baseAttribute.getGroupPath(), 
				new ArrayList<>(baseAttribute.getValues()),
				baseAttribute.getRemoteIdp(), baseAttribute.getTranslationProfile());
		this.direct = isDirect;
		this.creationTs = creationTs;
		this.updateTs = updateTs;
	}
	
	/**
	 * Cloning constructor. Deep cloning is performed.
	 * @param source
	 */
	public AttributeExt2(AttributeExt2 source, Date creationTs, Date updateTs)
	{
		this(source, source.isDirect(), creationTs, updateTs);
	}

	public AttributeExt2(Attribute2 baseAttribute, boolean isDirect)
	{
		this(baseAttribute, isDirect, null, null);
	}
	
	/**
	 * Cloning constructor. Deep cloning is performed.
	 * @param source
	 */
	public AttributeExt2(AttributeExt2 source)
	{
		this(source, source.isDirect(), source.creationTs, source.updateTs);
	}
	
	@JsonCreator
	public AttributeExt2(ObjectNode src)
	{
		super(src);
		fromJsonExt(src);
	}
	
	public AttributeExt2(String name, String valueSyntax, String groupPath, ObjectNode src)
	{
		super(name, valueSyntax, groupPath, src);
		fromJsonExt(src);
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

	@Override
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode ret = super.toJson();
		toJsonExt(ret);
		return ret;
	}

	@Override
	public ObjectNode toJsonBase()
	{
		ObjectNode ret = super.toJsonBase();
		toJsonExt(ret);
		return ret;
	}
	
	protected ObjectNode toJsonExt(ObjectNode root)
	{
		if (getCreationTs() != null)
			root.put("creationTs", getCreationTs().getTime());
		if (getUpdateTs() != null)
			root.put("updateTs", getUpdateTs().getTime());
		root.put("direct", direct);
		return root;
	}
	
	protected final void fromJsonExt(ObjectNode main)
	{
		if (main.has("creationTs"))
			setCreationTs(new Date(main.get("creationTs").asLong()));
		if (main.has("updateTs"))
			setUpdateTs(new Date(main.get("updateTs").asLong()));
		this.direct = main.get("direct").asBoolean(true);
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((creationTs == null) ? 0 : creationTs.hashCode());
		result = prime * result + (direct ? 1231 : 1237);
		result = prime * result + ((updateTs == null) ? 0 : updateTs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributeExt2 other = (AttributeExt2) obj;
		if (creationTs == null)
		{
			if (other.creationTs != null)
				return false;
		} else if (!creationTs.equals(other.creationTs))
			return false;
		if (direct != other.direct)
			return false;
		if (updateTs == null)
		{
			if (other.updateTs != null)
				return false;
		} else if (!updateTs.equals(other.updateTs))
			return false;
		return true;
	}
}
