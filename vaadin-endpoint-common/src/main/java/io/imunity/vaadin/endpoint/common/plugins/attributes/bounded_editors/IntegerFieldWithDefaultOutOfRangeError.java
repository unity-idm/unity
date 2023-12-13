/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors;

import com.vaadin.flow.component.textfield.IntegerField;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;

public class IntegerFieldWithDefaultOutOfRangeError extends IntegerField
{
	private final MessageSource msg;
	
	public IntegerFieldWithDefaultOutOfRangeError(MessageSource msg)
	{
		this.msg = msg;
	}
	
	@Override
	public void setErrorMessage(String errorMessage)
	{
		super.setErrorMessage(
				(errorMessage != null && errorMessage.isEmpty()) ? msg.getMessage("NumericAttributeHandler.rangeError",
						AttributeTypeUtils.getBoundsDesc(0, Integer.MAX_VALUE)) : errorMessage);
	}
	
}
