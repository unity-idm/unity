/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.confirmation;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestConfirmationInfo.Builder.class)
public class RestConfirmationInfo
{
	public final boolean confirmed;
	public final long confirmationDate;
	public final int sentRequestAmount;

	private RestConfirmationInfo(Builder builder)
	{
		this.confirmed = builder.confirmed;
		this.confirmationDate = builder.confirmationDate;
		this.sentRequestAmount = builder.sentRequestAmount;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(confirmationDate, confirmed, sentRequestAmount);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RestConfirmationInfo other = (RestConfirmationInfo) obj;
		return confirmationDate == other.confirmationDate && confirmed == other.confirmed
				&& sentRequestAmount == other.sentRequestAmount;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private boolean confirmed;
		private long confirmationDate;
		private int sentRequestAmount;

		private Builder()
		{
		}

		public Builder withConfirmed(boolean confirmed)
		{
			this.confirmed = confirmed;
			return this;
		}

		public Builder withConfirmationDate(long confirmationDate)
		{
			this.confirmationDate = confirmationDate;
			return this;
		}

		public Builder withSentRequestAmount(int sentRequestAmount)
		{
			this.sentRequestAmount = sentRequestAmount;
			return this;
		}

		public RestConfirmationInfo build()
		{
			return new RestConfirmationInfo(this);
		}
	}
}
