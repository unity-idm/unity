/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import java.util.List;
import java.util.Objects;

public class ActiveValueClient
{
	public final String key;
	public final String client;
	public final List<String> singleValueAttributes;
	public final List<String> multiValueAttributes;

	public ActiveValueClient(String key, String client, List<String> singleValueAttributes, List<String> multiValueAttributes)
	{
		this.key = key;
		this.client = client;
		this.singleValueAttributes = singleValueAttributes;
		this.multiValueAttributes = multiValueAttributes;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ActiveValueClient that = (ActiveValueClient) o;
		return Objects.equals(key, that.key) && Objects.equals(client, that.client) && Objects.equals(singleValueAttributes, that.singleValueAttributes) && Objects.equals(multiValueAttributes, that.multiValueAttributes);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key, client, singleValueAttributes, multiValueAttributes);
	}

	@Override
	public String toString()
	{
		return "ActiveValueClient{" +
				"key='" + key + '\'' +
				", client='" + client + '\'' +
				", singleValueAttributes=" + singleValueAttributes +
				", multiValueAttributes=" + multiValueAttributes +
				'}';
	}
}
