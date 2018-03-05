/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identitytype;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;
import pl.edu.icm.unity.webui.confirmations.EmailConfirmationConfigurationEditor;

/**
 * Allows to edit an identity type. It is only possible to edit description and self modifiable flag. 
 * 
 * @author K. Benedyczak
 */
public class IdentityTypeEditor extends FormLayout
{
	private UnityMessageSource msg;
	
	private IdentityType original;
	private AbstractTextField name;
	private TextArea description;
	private CheckBox selfModifiable;
	private TextField min;
	private TextField minVerified;
	private IntegerBoundEditor max;
	private Binder<IdentityType> binder;
	private IdentityTypeSupport idTypeSupport;
	private EmailConfirmationConfigurationEditor confirmationEditor;
	private MessageTemplateManagement msgTemplateMan;
	private IdentityTypeDefinition typeDefinition;

	public IdentityTypeEditor(UnityMessageSource msg, IdentityTypeSupport idTypeSupport, MessageTemplateManagement msgTemplateMan,
			IdentityType toEdit)
	{
		super();
		this.msg = msg;
		this.idTypeSupport = idTypeSupport;
		this.msgTemplateMan = msgTemplateMan;
		original = toEdit;

		initUI(toEdit);
	}

	private void initUI(IdentityType toEdit)
	{
		setWidth(100, Unit.PERCENTAGE);

		name = new TextField(msg.getMessage("IdentityType.name"));
		name.setReadOnly(true);

		addComponent(name);

		description = new TextArea(msg.getMessage("IdentityType.description"));
		description.setWidth(100, Unit.PERCENTAGE);
		addComponent(description);

		selfModifiable = new CheckBox(msg.getMessage("IdentityType.selfModificable"));
		selfModifiable.addValueChangeListener(e -> {
			refresh();
		});

		addComponent(selfModifiable);

		Label limInfo = new Label(msg.getMessage("IdentityType.limitsDescription"));
		addComponent(limInfo);

		min = new TextField(msg.getMessage("IdentityType.min"));
		addComponent(min);

		minVerified = new TextField(msg.getMessage("IdentityType.minVerified"));
		addComponent(minVerified);
		typeDefinition = idTypeSupport
				.getTypeDefinition(toEdit.getName());
		if (!typeDefinition.isEmailVerifiable())
		{	
			minVerified.setVisible(false);
		}

		max = new IntegerBoundEditor(msg, msg.getMessage("IdentityType.maxUnlimited"),
				msg.getMessage("IdentityType.max"), Integer.MAX_VALUE, 0, null);

		addComponent(max);

		if (typeDefinition.isEmailVerifiable())
		{
			confirmationEditor = new EmailConfirmationConfigurationEditor(toEdit.getEmailConfirmationConfiguration(), msg, msgTemplateMan);
			confirmationEditor.addFieldToLayout(this);
		}
				
		binder = new Binder<>(IdentityType.class);
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.forField(description).bind("description");

		binder.forField(selfModifiable).bind("selfModificable");
		max.configureBinding(binder, "maxInstances");
		binder.forField(min).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToIntegerConverter(
						msg.getMessage("IntegerBoundEditor.notANumber")))
				.withValidator(new IntegerRangeValidator(
						msg.getMessage("IdentityType.invalidNumber"), 0,
						Integer.MAX_VALUE))
				.bind("minInstances");
		binder.forField(minVerified).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToIntegerConverter(
						msg.getMessage("IntegerBoundEditor.notANumber")))
				.withValidator(new IntegerRangeValidator(
						msg.getMessage("IdentityType.invalidNumber"), 0,
						Integer.MAX_VALUE))
				.bind("minVerifiedInstances");

		binder.setBean(toEdit);		
		refresh();
	}

	private void refresh()
	{
		boolean state = selfModifiable.getValue();
		min.setEnabled(state);
		max.setEnabled(state);
		minVerified.setEnabled(state);
	}

	public IdentityType getIdentityType()
			throws FormValidationException
	{
		if (!binder.isValid())
			throw new FormValidationException();
		IdentityType ret = binder.getBean();
		ret.setIdentityTypeProvider(original.getIdentityTypeProvider());
		ret.setExtractedAttributes(original.getExtractedAttributes());
		if (typeDefinition.isEmailVerifiable())
			ret.setEmailConfirmationConfiguration(confirmationEditor.getCurrentValue());
		return ret;
	}
}
