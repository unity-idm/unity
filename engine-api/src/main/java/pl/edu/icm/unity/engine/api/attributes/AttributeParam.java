/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Object that encapsulates attribute with surrounding properties treated as a
 * whole from engine point of view.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class AttributeParam
{
	private EntityParam entity;
	private Attribute attribute;
	private String groupPath;
	private String attributeTypeId;

	public AttributeParam(EntityParam entity, Attribute attribute)
	{
		this.entity = entity;
		this.attribute = attribute;
	}

	public EntityParam getEntity()
	{
		return entity;
	}

	public void setEntity(EntityParam entity)
	{
		this.entity = entity;
	}

	public Attribute getAttribute()
	{
		return attribute;
	}

	public void setAttribute(Attribute attribute)
	{
		this.attribute = attribute;
	}

	public String getGroupPath()
	{
		return groupPath;
	}

	public void setGroupPath(String groupPath)
	{
		this.groupPath = groupPath;
	}

	public String getAttributeTypeId()
	{
		return attributeTypeId;
	}

	public void setAttributeTypeId(String attributeTypeId)
	{
		this.attributeTypeId = attributeTypeId;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder
	{
		private EntityParam entity;
		private Attribute attribute;
		private String groupPath;
		private String attributeTypeId;

		public Builder withEntity(EntityParam entity)
		{
			this.entity = entity;
			return this;
		}

		public Builder withAttribute(Attribute attribute)
		{
			this.attribute = attribute;
			return this;
		}

		public Builder withGroupPath(String groupPath)
		{
			this.groupPath = groupPath;
			return this;
		}

		public Builder withAttributeTypeId(String attributeTypeId)
		{
			this.attributeTypeId = attributeTypeId;
			return this;
		}

		public AttributeParam build()
		{
			AttributeParam engineAttr = new AttributeParam(entity, attribute);
			engineAttr.setAttributeTypeId(attributeTypeId);
			engineAttr.setGroupPath(groupPath);
			return engineAttr;
		}
	}
}