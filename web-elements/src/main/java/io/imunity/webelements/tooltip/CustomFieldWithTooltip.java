/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package io.imunity.webelements.tooltip;

import java.util.function.Supplier;

import com.vaadin.data.HasValue;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;

class CustomFieldWithTooltip<VALUE, FIELD extends Component & HasValue<VALUE>> extends CustomField<VALUE>
{
	private final FIELD field;
	private final HorizontalLayout content;
	
	CustomFieldWithTooltip(Supplier<FIELD> ctor, 
			String caption, 
			String tooltipInfo)
	{
		this.field = ctor.get();
		this.field.setWidth(100, Unit.PERCENTAGE);
		
		TooltipElement tooltipElement = new TooltipElement(tooltipInfo);
		
		this.content = new HorizontalLayout();
		this.content.setMargin(false);
		this.content.setSpacing(false);
		this.content.setWidth(100, Unit.PERCENTAGE);
		this.content.addComponents(field, tooltipElement);
		this.content.setExpandRatio(field, 1);
		this.content.setExpandRatio(tooltipElement, 0);
		
		setCaption(caption);
	}

	@Override
	public VALUE getValue()
	{
		return field.getValue();
	}

	@Override
	protected Component initContent()
	{
		return content;
	}

	@Override
	protected void doSetValue(VALUE value)
	{
		field.setValue(value);
	}
	
	protected FIELD getField()
	{
		return field;
	}

	@Override
	public VALUE getEmptyValue()
	{
		return field.getEmptyValue();
	}
}
