/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Implementations are used to edit values of translation action parameters.
 * @author K. Benedyczak
 */
public interface ActionParameterComponent
{
	String getActionValue();
	String getLabel();
	void setActionValue(String value);
	boolean isValid();
	void setReadOnly(boolean readOnly);
	void addValueChangeCallback(Runnable callback);
	default String getActionValueRepresentation(MessageSource msg)
	{
		return getActionValue();
	}
}
