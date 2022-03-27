/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.AbstractField;

public interface FieldSizeConstans
{
	static final int WIDE_FIELD_WIDTH = 50;
	static final Unit WIDE_FIELD_WIDTH_UNIT = Unit.EM;

	static final int LINK_FIELD_WIDTH = WIDE_FIELD_WIDTH;
	static final Unit LINK_FIELD_WIDTH_UNIT = WIDE_FIELD_WIDTH_UNIT;
	
	static final int MEDIUM_FIELD_WIDTH = 30;
	static final Unit MEDIUM_FIELD_WIDTH_UNIT = Unit.EM;
	
	static final int SHORT_FIELD_WIDTH = 18;
	static final Unit SHORT_FIELD_WIDTH_UNIT = Unit.EM;

	default void setDescriptionWith(AbstractField<?> field)
	{
		field.setWidth(WIDE_FIELD_WIDTH, WIDE_FIELD_WIDTH_UNIT);
	}
}
