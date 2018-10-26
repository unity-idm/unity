/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.Map;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import io.imunity.webconsole.common.AbstractConfirmView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;

/**
 * Base for realm editor.
 * 
 * @author P.Piernik
 *
 */
//FIXME -> use composition instead of inheritance. So that you can have ConfirmView, which takes come 
//other component as its contents. 
public abstract class AbstractEditRealm extends AbstractConfirmView
{
	protected Binder<AuthenticationRealm> binder;

	protected TextField name;

	private TextArea description;

	private IntStepper blockFor;

	private IntStepper blockAfterUnsuccessfulLogins;

	private IntStepper maxInactivity;

	private IntStepper allowForRememberMeDays;

	private ComboBox<RememberMePolicy> rememberMePolicy;

	protected RealmController controller;

	public AbstractEditRealm(UnityMessageSource msg, RealmController controller)
	{
		super(msg, msg.getMessage("ok"), msg.getMessage("cancel"));
		this.controller = controller;
	}

	@Override
	protected Component getContents(Map<String, String> parameters) throws Exception
	{
		name = new TextField(msg.getMessage("RealmEditor.name"));

		description = new DescriptionTextArea(msg.getMessage("RealmEditor.description"));

		blockFor = new IntStepper(msg.getMessage("RealmEditor.blockFor"));
		blockFor.setMinValue(1);
		blockFor.setMaxValue(999);
		blockFor.setWidth(4, Unit.EM);

		blockAfterUnsuccessfulLogins = new IntStepper(
				msg.getMessage("RealmEditor.blockAfterUnsuccessfulLogins"));
		blockAfterUnsuccessfulLogins.setMinValue(1);
		blockAfterUnsuccessfulLogins.setMaxValue(999);
		blockAfterUnsuccessfulLogins.setWidth(4, Unit.EM);

		allowForRememberMeDays = new IntStepper(
				msg.getMessage("RealmEditor.allowForRememberMeDays"));
		allowForRememberMeDays.setMinValue(1);
		allowForRememberMeDays.setMaxValue(999);
		allowForRememberMeDays.setWidth(4, Unit.EM);

		rememberMePolicy = new ComboBox<>(msg.getMessage("RealmEditor.rememberMePolicy"));
		rememberMePolicy.setItems(RememberMePolicy.values());
		rememberMePolicy.setEmptySelectionAllowed(false);

		maxInactivity = new IntStepper(msg.getMessage("RealmEditor.maxInactivity"));
		maxInactivity.setMinValue(1);
		maxInactivity.setMaxValue(9999);
		maxInactivity.setWidth(4, Unit.EM);

		binder = new Binder<>(AuthenticationRealm.class);

		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.bind(description, "description");

		binder.forField(blockFor).asRequired(msg.getMessage("fieldRequired"))
				.bind("blockFor");
		binder.forField(blockAfterUnsuccessfulLogins)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind("blockAfterUnsuccessfulLogins");
		binder.forField(allowForRememberMeDays).asRequired(msg.getMessage("fieldRequired"))
				.bind("allowForRememberMeDays");

		binder.forField(rememberMePolicy).asRequired(msg.getMessage("fieldRequired"))
				.bind("rememberMePolicy");

		binder.forField(maxInactivity).asRequired(msg.getMessage("fieldRequired"))
				.bind("maxInactivity");
		init(parameters);
		FormLayout mainLayout = new FormLayout();

		mainLayout.addComponents(name, description, blockFor, blockAfterUnsuccessfulLogins,
				allowForRememberMeDays, rememberMePolicy, maxInactivity);
		return mainLayout;
	}

	protected abstract void init(Map<String, String> parameters) throws Exception;
}
