/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.bounded_editors;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;

public class IntegerBoundEditor extends CustomField<Integer>
{
	protected MessageSource msg;
	protected Integer min;
	protected Integer max;
	protected Integer bound;

	private Checkbox unlimited;
	private IntegerField limit;
	
	public IntegerBoundEditor(MessageSource msg, String labelUnlimited,
			Integer bound, Integer min, Integer max)
	{
		this.msg = msg;
		this.bound = bound;
		this.min = min;
		this.max = max;
		
		unlimited = new Checkbox();
		unlimited.setLabel(labelUnlimited);
		limit = new IntegerField();
		if (max != null)
			limit.setMax(max);
		if (min != null)
			limit.setMin(min);
		limit.setStepButtonsVisible(true);
		unlimited.addValueChangeListener(event ->
		{
			boolean limited = !unlimited.getValue();
			limit.setEnabled(limited);
		});
		
		
		HorizontalLayout hl = new HorizontalLayout();
		hl.add(limit, unlimited);
		hl.setAlignSelf(Alignment.CENTER, limit);
		hl.setAlignSelf(Alignment.CENTER, unlimited);

		hl.setSpacing(true);
		add(hl);
	}
	
	public void setReadOnly()
	{
		unlimited.setReadOnly(true);
		limit.setReadOnly(true);
	}

	@Override
	protected Integer generateModelValue()
	{
		return unlimited.getValue() ? bound : limit.getValue();
	}

	@Override
	protected void setPresentationValue(Integer newPresentationValue)
	{
		unlimited.setValue(newPresentationValue.equals(bound));
		limit.setValue(newPresentationValue.equals(bound) ? 0 : newPresentationValue);
	}
	
	
	public void configureBinding(Binder<?> binder, String fieldName)
	{
		binder.forField(this)
			.withValidator(getValidator(msg, min, max))
			.bind(fieldName);
	}
	
	private static Validator<Integer> getValidator(MessageSource msg, Integer min, Integer max)
	{
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		return new IntegerRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range), 
				min, max);		
	}
}