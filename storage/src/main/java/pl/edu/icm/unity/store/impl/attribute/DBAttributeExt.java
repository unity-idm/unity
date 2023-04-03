/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBAttributeExt.Builder.class)
public class DBAttributeExt extends DBAttribute
{
	public final boolean direct;
	public final Date creationTs;
	public final Date updateTs;

	private DBAttributeExt(Builder builder)
	{
		super(builder);
		this.direct = builder.direct;
		this.creationTs = builder.creationTs;
		this.updateTs = builder.updateTs;
	}

	public static Builder builder()
	{
		return new Builder();
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder extends DBAttributeBuilder<Builder>
	{
		private boolean direct;
		private Date creationTs;
		private Date updateTs;

		public Builder()
		{
		}

		public Builder withDirect(boolean direct)
		{
			this.direct = direct;
			return this;
		}

		public Builder withCreationTs(Date creationTs)
		{
			this.creationTs = creationTs;
			return this;
		}

		public Builder withUpdateTs(Date updateTs)
		{
			this.updateTs = updateTs;
			return this;
		}

		public DBAttributeExt build()
		{
			return new DBAttributeExt(this);
		}
	}

}
