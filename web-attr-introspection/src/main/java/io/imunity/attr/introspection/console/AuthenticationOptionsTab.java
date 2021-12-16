/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import java.util.List;
import java.util.function.Supplier;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.console.services.tabs.WebServiceAuthnScreenLayoutEditor;

class AuthenticationOptionsTab extends CustomComponent implements EditorTab
{
	private final MessageSource msg;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final Supplier<List<String>> authnOptionSupplier;
	private WebServiceAuthnScreenLayoutEditor webScreenEditor;
	private Binder<AttrIntrospectionAuthnScreenConfiguration> authnScreenConfigBinder;
	
	
	AuthenticationOptionsTab(MessageSource msg, AuthenticatorSupportService authenticatorSupportService,
			Supplier<List<String>> authnOptionSupplier)
	{
		this.msg = msg;
		this.authenticatorSupportService = authenticatorSupportService;
		this.authnOptionSupplier = authnOptionSupplier;
	}
	
	void initUI(Binder<AttrIntrospectionAuthnScreenConfiguration> authnScreenConfigBinder)
	{
		this.authnScreenConfigBinder = authnScreenConfigBinder;
		setIcon(Images.sign_in.getResource());
		setCaption(msg.getMessage("AuthenticationOptionsTab.authnenticationOptions"));
		VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.addComponent(buildScreenLayoutSection());
		setCompositionRoot(mainWrapper);
	}
	
	private Component buildScreenLayoutSection()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		
		CheckBox enableSearch = new CheckBox();
		enableSearch.setCaption(msg.getMessage("AuthenticationOptionsTab.enableSearch"));
		authnScreenConfigBinder.forField(enableSearch).bind("enableSearch");
		main.addComponent(enableSearch);
		
		webScreenEditor = new WebServiceAuthnScreenLayoutEditor(msg, authenticatorSupportService,
				authnOptionSupplier);
		webScreenEditor.setRegistrationEnabled(false);
		webScreenEditor.setLastUsedOptionEnabled(false);
		webScreenEditor.setAddColumnEnabled(false);
		webScreenEditor.configureBinding(authnScreenConfigBinder, "authnLayoutConfiguration");
		main.addComponent(webScreenEditor);
		return main;
	}
	
	
	@Override
	public String getType()
	{
		return ServiceEditorTab.AUTHENTICATION.toString();
	}
	
	@Override
	public Component getComponent()
	{
		return this;
	}
}
