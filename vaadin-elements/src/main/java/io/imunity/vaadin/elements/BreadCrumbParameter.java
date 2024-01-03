/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import java.util.Objects;

public class BreadCrumbParameter
{
	public static final String BREAD_CRUMB_SEPARATOR = " > ";

	public final String id;
	public final String name;
	public final String parameter;
	public final boolean disable;

	public BreadCrumbParameter(String id, String name, String parameter)
	{
		this(id, name, parameter, false);
	}

	public BreadCrumbParameter(String id, String name, String parameter, boolean disable)
	{
		this.id = id;
		this.name = name;
		this.parameter = parameter;
		this.disable = disable;
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
