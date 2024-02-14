/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt_authn;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.MoreObjects;

public class TestDTO
{
	public final String field;

	@JsonCreator
	public TestDTO(String field)
	{
		this.field = field;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(field);
	}

	@Override
	public boolean equals(Object object)
	{
		if (object instanceof TestDTO)
		{
			TestDTO that = (TestDTO) object;
			return Objects.equals(this.field, that.field);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this).add("field", field).toString();
	}
}
