/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.identity_types;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import io.imunity.vaadin.endpoint.common.confirmations.EmailConfirmationConfigurationEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors.IntegerBoundEditor;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.webui.common.FormValidationException;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

/**
 * Allows to edit an identity type. It is only possible to edit description and
 * self modifiable flag.
 * 
 * @author K. Benedyczak
 */
class IdentityTypeEditor extends FormLayout
{
	private final MessageSource msg;
	private final IdentityType original;
	private final IdentityTypeSupport idTypeSupport;
	private final MessageTemplateManagement msgTemplateMan;

	private TextField name;
	private TextArea description;
	private Checkbox selfModifiable;
	private IntegerField min;
	private IntegerField minVerified;
	private IntegerBoundEditor max;
	private Binder<IdentityType> binder;
	private EmailConfirmationConfigurationEditor confirmationEditor;
	private IdentityTypeDefinition typeDefinition;

	IdentityTypeEditor(MessageSource msg, IdentityTypeSupport idTypeSupport, MessageTemplateManagement msgTemplateMan,
			IdentityType toEdit)
	{
		super();
		this.msg = msg;
		this.idTypeSupport = idTypeSupport;
		this.msgTemplateMan = msgTemplateMan;
		this.original = toEdit;
		initUI(toEdit);
	}

	private void initUI(IdentityType toEdit)
	{
		setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());

		name = new TextField();
		name.setReadOnly(true);
		name.setWidth(TEXT_FIELD_MEDIUM.value());
		addFormItem(name, msg.getMessage("IdentityType.name"));

		description = new TextArea();
		description.setWidthFull();
		addFormItem(description, msg.getMessage("IdentityType.description"));

		selfModifiable = new Checkbox();
		selfModifiable.addValueChangeListener(e ->
		{
			refresh();
		});

		addFormItem(selfModifiable, msg.getMessage("IdentityType.selfModificable"));

		NativeLabel limInfo = new NativeLabel(msg.getMessage("IdentityType.limitsDescription"));
		addFormItem(limInfo, "");

		min = new IntegerField();
		min.setStepButtonsVisible(true);
		min.setMin(0);
	
	
		addFormItem(min, msg.getMessage("IdentityType.min"));

		minVerified = new IntegerField();
		minVerified.setStepButtonsVisible(true);
		minVerified.setMin(0);
	
		typeDefinition = idTypeSupport.getTypeDefinition(toEdit.getName());
		if (typeDefinition.isEmailVerifiable())
		{
			addFormItem(minVerified, msg.getMessage("IdentityType.minVerified"));
		}

		max = new IntegerBoundEditor(msg, msg.getMessage("IdentityType.maxUnlimited"), Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
		addFormItem(max, msg.getMessage("IdentityType.max"));
		
		if (typeDefinition.isEmailVerifiable())
		{
			confirmationEditor = new EmailConfirmationConfigurationEditor(toEdit.getEmailConfirmationConfiguration(),
					msg, msgTemplateMan);
			confirmationEditor.addFieldToLayout(this);
		}

		binder = new Binder<>(IdentityType.class);
		binder.forField(name)
				.bind("name");
		binder.forField(description)
				.bind("description");

		binder.forField(selfModifiable)
				.bind("selfModificable");
		max.configureBinding(binder, "maxInstances");
		binder.forField(min)
				.asRequired(msg.getMessage("fieldRequired"))
				.withValidator(
						new IntegerRangeValidator(msg.getMessage("IdentityType.invalidNumber"), 0, Integer.MAX_VALUE))
				.bind("minInstances");
		binder.forField(minVerified)
				.asRequired(msg.getMessage("fieldRequired"))
				.withValidator(
						new IntegerRangeValidator(msg.getMessage("IdentityType.invalidNumber"), 0, Integer.MAX_VALUE))
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

	IdentityType getIdentityType() throws FormValidationException
	{
		if (!binder.isValid())
			throw new FormValidationException();
		IdentityType ret = binder.getBean();
		ret.setIdentityTypeProvider(original.getIdentityTypeProvider());
		if (typeDefinition.isEmailVerifiable())
			ret.setEmailConfirmationConfiguration(confirmationEditor.getCurrentValue());
		return ret;
	}
}
