/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;

import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

class FederatedPrivateKeyJwtAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private final MessageSource msg;

	FederatedPrivateKeyJwtAuthenticatorEditor(MessageSource msg)
	{
		super(msg);
		this.msg = msg;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		init(msg.getMessage("FederatedPrivateKeyJwtAuthenticatorEditor.defaultName"), toEdit, forceNameEditable);

		FormLayout form = new FormLayout();
		form.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		form.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));

		return form;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), FederatedPrivateKeyJwtVerificator.NAME, "{}", null);
	}
}
