/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations.editor;

public class MessageParam
{
	private String name;
	private String value;
	private boolean nameEditable;

	public MessageParam(String name, String value, boolean nameEditable)
	{
		this.name = name;
		this.value = value;
		this.nameEditable = nameEditable;
	}

	public MessageParam()
	{
		this("", "", true);
	}

	public boolean isNameEditable()
	{
		return nameEditable;
	}

	public void setNameEditable(boolean nameEditable)
	{
		this.nameEditable = nameEditable;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}