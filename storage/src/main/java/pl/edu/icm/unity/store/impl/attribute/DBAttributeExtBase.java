/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBAttributeExtBase.Builder.class)
class DBAttributeExtBase extends DBAttributeBase
{
	public final boolean direct;
	public final Date creationTs;
	public final Date updateTs;

	private DBAttributeExtBase(Builder builder)
	{
		super(builder);
		this.direct = builder.direct;
		this.creationTs = builder.creationTs;
		this.updateTs = builder.updateTs;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(creationTs, direct, updateTs);
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
		DBAttributeExtBase other = (DBAttributeExtBase) obj;
		return Objects.equals(creationTs, other.creationTs) && direct == other.direct
				&& Objects.equals(updateTs, other.updateTs);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder extends DBAttributeBaseBuilder<Builder>
	{
		private boolean direct;
		private Date creationTs;
		private Date updateTs;

		public Builder()
		{
		}

		Builder withDirect(boolean direct)
		{
			this.direct = direct;
			return this;
		}

		Builder withCreationTs(Date creationTs)
		{
			this.creationTs = creationTs;
			return this;
		}

		Builder withUpdateTs(Date updateTs)
		{
			this.updateTs = updateTs;
			return this;
		}

		public DBAttributeExtBase build()
		{
			return new DBAttributeExtBase(this);
		}
	}

}
