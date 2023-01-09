/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.policy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class RestPolicyDocumentId
{
	public final long id;

	@JsonCreator
	public RestPolicyDocumentId(@JsonProperty("id") long id)
	{
		this.id = id;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestPolicyDocumentId that = (RestPolicyDocumentId) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id);
	}

	@Override
	public String toString()
	{
		return "RestPolicyDocumentId{" +
			"id=" + id +
			'}';
	}
}
