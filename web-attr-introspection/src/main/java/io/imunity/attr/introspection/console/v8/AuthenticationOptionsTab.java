/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console.v8;

import java.util.List;
import java.util.function.Supplier;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.file.ImageField;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.console.services.tabs.WebServiceAuthnScreenLayoutEditor;

class AuthenticationOptionsTab extends CustomComponent implements EditorTab
{
	private final MessageSource msg;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final Supplier<List<String>> authnOptionSupplier;
	private final URIAccessService uriAccessService;
	private final UnityServerConfiguration serverConfig;
	
	private WebServiceAuthnScreenLayoutEditor webScreenEditor;
	private Binder<AttrIntrospectionAuthnScreenConfiguration> authnScreenConfigBinder;
	
	
	AuthenticationOptionsTab(MessageSource msg, AuthenticatorSupportService authenticatorSupportService,
			Supplier<List<String>> authnOptionSupplier, 
			URIAccessService uriAccessService,
			UnityServerConfiguration serverConfig)
	{
		this.msg = msg;
		this.authenticatorSupportService = authenticatorSupportService;
		this.authnOptionSupplier = authnOptionSupplier;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
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
		
		FormLayout wrapper = new FormLayoutWithFixedCaptionWidth();
		wrapper.setMargin(false);
		
		ImageField logo = new ImageField(msg, uriAccessService, serverConfig.getFileSizeLimit());
		logo.setCaption(msg.getMessage("AuthenticationOptionsTab.logo"));
		logo.configureBinding(authnScreenConfigBinder, "logo");
		wrapper.addComponent(logo);

		I18nTextField title = new I18nTextField(msg);
		title.setCaption(msg.getMessage("AuthenticationOptionsTab.title"));
		authnScreenConfigBinder.forField(title).bind("title");
		wrapper.addComponent(title);
		
		CheckBox enableSearch = new CheckBox();
		enableSearch.setCaption(msg.getMessage("AuthenticationOptionsTab.enableSearch"));
		authnScreenConfigBinder.forField(enableSearch).bind("enableSearch");
		wrapper.addComponent(enableSearch);
		
		main.addComponent(wrapper);
		
		
		
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
