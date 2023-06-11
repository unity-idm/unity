/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directorySetup.attributeClasses;

import com.vaadin.ui.TwinColSelect;

import pl.edu.icm.unity.base.attribute.AttributesClass;

/**
 * Customization of the {@link TwinColSelect} for {@link AttributesClass} selection.
 * @author K. Benedyczak
 */
public class ACTwinColSelect extends TwinColSelect<String>
{
	public ACTwinColSelect(String leftCaption, String rightCaption)
	{
		this("", leftCaption, rightCaption);
	}

	public ACTwinColSelect(String caption, String leftCaption, String rightCaption)
	{
		setCaption(caption);
		setLeftColumnCaption(leftCaption);
		setRightColumnCaption(rightCaption);
		setWidth(90, Unit.PERCENTAGE);
		setRows(5);
	}
}
