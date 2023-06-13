/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.tabs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.chips.GroupedValuesChipsWithDropdown;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;

/**
 * Service base authentication editor tab
 * 
 * @author P.Piernik
 *
 */
public class AuthenticationTab extends CustomComponent implements EditorTab
{
	private MessageSource msg;
	private List<String> allRealms;
	private List<String> flows;
	private List<String> authenticators;

	public AuthenticationTab(MessageSource msg, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, List<String> allRealms, String binding)

	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.flows = WebServiceAuthenticationTab.filterBindingCompatibleAuthenticationFlow(flows, authenticators, binding);
		this.authenticators = authenticators.stream().filter(a -> a.getSupportedBindings().contains(binding))
				.map(a -> a.getId()).collect(Collectors.toList());
	}

	public void initUI(Binder<DefaultServiceDefinition> binder)
	{	
		setCaption(msg.getMessage("ServiceEditorBase.authentication"));
		setIcon(Images.sign_in.getResource());
		
		FormLayoutWithFixedCaptionWidth mainAuthenticationLayout = new FormLayoutWithFixedCaptionWidth();
		ComboBox<String> realm = new ComboBox<>();
		realm.setCaption(msg.getMessage("ServiceEditorBase.realm"));
		realm.setItems(allRealms);
		realm.setEmptySelectionAllowed(false);
		binder.forField(realm).asRequired().bind("realm");
		mainAuthenticationLayout.addComponent(realm);

		Map<String, List<String>> labels = new HashMap<>();
		labels.put(msg.getMessage("ServiceEditorBase.flows"), flows);
		labels.put(msg.getMessage("ServiceEditorBase.authenticators"), authenticators);
		GroupedValuesChipsWithDropdown authAndFlows = new GroupedValuesChipsWithDropdown(labels);
		authAndFlows.setCaption(msg.getMessage("ServiceEditorBase.authenticatorsAndFlows"));
		binder.forField(authAndFlows).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}

			return ValidationResult.ok();

		}).bind("authenticationOptions");
		authAndFlows.setRequiredIndicatorVisible(true);
		mainAuthenticationLayout.addComponent(authAndFlows);
		setCompositionRoot(mainAuthenticationLayout);
	}

	@Override
	public String getType()
	{
		return ServiceEditorTab.AUTHENTICATION.toString();
	}

	@Override
	public CustomComponent getComponent()
	{
		return this;
	}
}
