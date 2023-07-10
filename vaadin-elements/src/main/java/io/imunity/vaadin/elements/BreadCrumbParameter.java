/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import java.util.Objects;

public class BreadCrumbParameter
{
	public final String id;
	public final String name;
	public final String parameter;

	public BreadCrumbParameter(String id, String name, String parameter)
	{
		this.id = id;
		this.name = name;
		this.parameter = parameter;
	}

	public BreadCrumbParameter(String id, String name)
	{
		this(id, name, null);
	}

	public BreadCrumbParameter(String name)
	{
		this(null, name, null);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BreadCrumbParameter that = (BreadCrumbParameter) o;
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
		return "BreadCrumbParameter{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", parameter='" + parameter + '\'' +
			'}';
	}
}
