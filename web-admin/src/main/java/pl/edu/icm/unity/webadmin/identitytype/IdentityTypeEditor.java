/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identitytype;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.FormValidator;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

/**
 * Allows to edit an identity type. It is only possible to edit description and self modifiable flag. 
 * 
 * @author K. Benedyczak
 */
public class IdentityTypeEditor extends FormLayout
{
	private UnityMessageSource msg;
	
	private IdentityType original;
	private Label name;
	private TextArea description;
	private CheckBox selfModifiable;
	private TextField min;
	private TextField minVerified;
	private IntegerBoundEditor max;
	private FormValidator validator;
	
	public IdentityTypeEditor(UnityMessageSource msg, IdentityType toEdit)
	{
		super();
		this.msg = msg;
		original = toEdit;
		
		initUI(toEdit);
	}

	private void initUI(IdentityType toEdit)
	{
		setWidth(100, Unit.PERCENTAGE);

		name = new Label(toEdit.getIdentityTypeProvider().getId());
		name.setCaption(msg.getMessage("IdentityType.name"));
		addComponent(name);
		
		description = new TextArea(msg.getMessage("IdentityType.description"));
		description.setWidth(100, Unit.PERCENTAGE);
		addComponent(description);
		
		selfModifiable = new CheckBox(msg.getMessage("IdentityType.selfModificable"));
		selfModifiable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				boolean state = selfModifiable.getValue();
				min.setEnabled(state);
				max.setEnabled(state);
				minVerified.setEnabled(state);
			}
		});
		addComponent(selfModifiable);

		Label limInfo = new Label(msg.getMessage("IdentityType.limitsDescription"));
		addComponent(limInfo);
		
		min = new TextField(msg.getMessage("IdentityType.min"));
		min.setConverter(new StringToIntegerConverter());
		min.setConvertedValue(toEdit.getMinInstances());
		min.setNullRepresentation("");
		min.addValidator(new IntegerRangeValidator(msg.getMessage("IdentityType.invalidNumber"), 
				0, Integer.MAX_VALUE));
		addComponent(min);
		
		minVerified = new TextField(msg.getMessage("IdentityType.minVerified"));
		minVerified.setConverter(new StringToIntegerConverter());
		minVerified.setNullRepresentation("");
		minVerified.addValidator(new IntegerRangeValidator(msg.getMessage("IdentityType.invalidNumber"), 
				0, Integer.MAX_VALUE));
		minVerified.setConvertedValue(toEdit.getMinVerifiedInstances());
		addComponent(minVerified);
		if (!toEdit.getIdentityTypeProvider().isVerifiable())
			minVerified.setVisible(false);
		
		max = new IntegerBoundEditor(msg, msg.getMessage("IdentityType.maxUnlimited"), 
				msg.getMessage("IdentityType.max"), Integer.MAX_VALUE);
		max.setValue(toEdit.getMaxInstances());
		max.setMin(0);
		addComponent(max);

		validator = new FormValidator(this);
		
		setInitialValues(toEdit);
	}
	
	private void setInitialValues(IdentityType aType)
	{
		description.setValue(aType.getDescription());
		selfModifiable.setValue(aType.isSelfModificable());
	}
	
	public IdentityType getIdentityType() throws FormValidationException
	{
		validator.validate();
		
		IdentityType ret = new IdentityType(original.getIdentityTypeProvider());
		ret.setDescription(description.getValue());
		ret.setSelfModificable(selfModifiable.getValue());
		ret.setExtractedAttributes(original.getExtractedAttributes());
		ret.setMinInstances((Integer) min.getConvertedValue());
		ret.setMaxInstances(max.getValue());
		ret.setMinVerifiedInstances((Integer) minVerified.getConvertedValue());
		return ret;
	}
}
