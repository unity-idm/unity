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
public class EngineAttribute
{
	private EntityParam entity;
	private Attribute attribute;

	public EngineAttribute(EntityParam entity, Attribute attribute)
	{
		super();
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

	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder
	{
		private EntityParam entity;
		private Attribute attribute;

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

		public EngineAttribute build()
		{
			EngineAttribute engineAttr = new EngineAttribute(entity, attribute);
			return engineAttr;
		}
	}
}
