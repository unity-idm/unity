/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.registration.layout;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;

/**
 * represents a button with custom caption that handles external signup.
 */
public class FormRemoteSignupElement extends FormElement
{
	private I18nString prefix;
	private int index;

	public FormRemoteSignupElement(I18nString prefix, int index)
	{
		super(FormLayoutType.REMOTE_SIGNUP, true);
		this.prefix = prefix;
		this.index = index;
	}

	@JsonCreator
	private FormRemoteSignupElement()
	{
		super(FormLayoutType.REMOTE_SIGNUP, true);
	}

	public I18nString getPrefix()
	{
		return prefix;
	}

	public void setPrefix(I18nString prefix)
	{
		this.prefix = prefix;
	}

	public void setIndex(int index)
	{
		this.index = index;
	}

	public int getIndex()
	{
		return index;
	}

	@Override
	public String toString(MessageSource msg)
	{
		return "Remote Signup button '" + prefix.getValue(msg) + "'";
	}

	@Override
	public String toString()
	{
		return "Remote Signup button '" + prefix + "'";
	}

	@Override
	public boolean equals(final Object other)
	{
		if (this == other)
			return true;
		if (!(other instanceof FormRemoteSignupElement))
			return false;
		if (!super.equals(other))
			return false;
		FormRemoteSignupElement castOther = (FormRemoteSignupElement) other;
		return Objects.equals(prefix, castOther.prefix) && Objects.equals(index, castOther.index);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), prefix, index);
	}
}
