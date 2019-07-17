/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.endpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Functions;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.endpoint.EndpointPathValidator;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.chips.GroupedValuesChipsWithDropdown;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

/**
 * 
 * @author P.Piernik
 *
 */
public abstract class ServiceEditorBase extends CustomComponent implements ServiceEditorComponent
{
	protected UnityMessageSource msg;
	private TabSheet tabs;
	private Tab generalTab;
	private Tab authenticationTab;

	private VerticalLayout generalLayout;
	private VerticalLayout authenticationLayout;
	private FormLayoutWithFixedCaptionWidth mainAuthenticationLayout;

	private Binder<ServiceDefinition> binder;

	protected ServiceDefinition toEdit;
	protected boolean editMode;
	private List<String> allRealms;
	private List<String> flows;
	private List<String> authenticators;
	private EndpointTypeDescription type;

	protected GroupedValuesChipsWithDropdown authAndFlows;

	public ServiceEditorBase(UnityMessageSource msg, EndpointTypeDescription type, ServiceDefinition toEdit,
			List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators)
	{
		this.msg = msg;
		this.type = type;
		this.allRealms = allRealms;
		this.flows = filterAuthenticationFlow(flows, authenticators);
		this.authenticators = authenticators.stream()
				.filter(a -> a.getSupportedBindings().contains(type.getSupportedBinding()))
				.map(a -> a.getId()).collect(Collectors.toList());
		this.editMode = toEdit != null;
		this.toEdit = editMode ? toEdit : new ServiceDefinition(type.getName());
		binder = new Binder<>(ServiceDefinition.class);
		initUI();
		binder.setBean(this.toEdit);
	}

	private List<String> filterAuthenticationFlow(List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators)
	{
		List<String> filteredFlows = new ArrayList<>();
		Map<String, AuthenticatorInfo> authenticatorsMap = authenticators.stream()
				.collect(Collectors.toMap(AuthenticatorInfo::getId, Functions.identity()));

		for (AuthenticationFlowDefinition f : flows)
		{
			boolean add = true;
			for (String authenticatorName : f.getAllAuthenticators())
			{
				if (!(authenticatorsMap.get(authenticatorName)).getSupportedBindings()
						.contains(type.getSupportedBinding()))
				{
					add = false;

				}
			}
			if (add)
			{
				filteredFlows.add(f.getName());
			}
		}

		return filteredFlows;

	}

	public ServiceDefinition getServicetDefiniton() throws FormValidationException
	{
		boolean hasErrors = binder.validate().hasErrors();
		try
		{
			validateConfiguration();
		} catch (FormValidationException e)
		{
			hasErrors = true;
		}

		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		ServiceDefinition bean = binder.getBean();
		bean.setConfiguration(getConfiguration(bean.getName()));
		return bean;
	}

	private void setErrorInTabs()
	{
		generalTab.setComponentError(null);
		authenticationTab.setComponentError(null);
		
		if (assertErrorComponent(generalLayout))
		{
			generalTab.setComponentError(new UserError(msg.getMessage("error")));
		}

		if (assertErrorComponent(authenticationLayout))
		{
			authenticationTab.setComponentError(new UserError(msg.getMessage("error")));
		}
	}

	boolean assertErrorComponent(Component component)
	{

		if (component instanceof AbstractComponent)
		{
			AbstractComponent ac = (AbstractComponent) component;
			if (ac.getComponentError() != null)
				return true;
		}

		if (component instanceof HasComponents)
		{
			HasComponents ac = (HasComponents) component;
			Iterator<Component> it = ac.iterator();
			while (it.hasNext())
			{
				if (assertErrorComponent(it.next()))
				{
					return true;
				}
			}
		}

		return false;
	}

	protected void addToGeneralTab(Component... c)
	{
		generalLayout.addComponents(c);
	}

	protected void addToAuthenticationTab(boolean generalSection, Component... c)
	{
		if (!generalSection)
		{
			authenticationLayout.addComponents(c);
		} else
		{
			mainAuthenticationLayout.addComponents(c);
		}
	}

	@Override
	public void setActiveTab(ServiceEditorTab tab)
	{
		if (tab.equals(ServiceEditorTab.AUTHENTICATION))
		{
			tabs.setSelectedTab(authenticationTab);
		} else
		{
			tabs.setSelectedTab(generalTab);
		}
	}

	protected abstract String getConfiguration(String serviceName) throws FormValidationException;
	protected abstract void validateConfiguration() throws FormValidationException;

	private void initUI()
	{
		tabs = new TabSheet();
		generalTab = tabs.addTab(buildGeneralTab(), msg.getMessage("ServiceEditorBase.general"));
		generalTab.setIcon(Images.cogs.getResource());
		authenticationTab = tabs.addTab(buildAuthenticationTab(),
				msg.getMessage("ServiceEditorBase.authentication"));
		authenticationTab.setIcon(Images.sign_in.getResource());
		tabs.addSelectedTabChangeListener(e -> {
			setErrorInTabs();
		});
		setCompositionRoot(tabs);

	}

	protected VerticalLayout buildGeneralTab()
	{
		generalLayout = new VerticalLayout();
		generalLayout.setMargin(false);
		generalLayout.setSpacing(false);

		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();

		TextField name = new TextField();
		name.setCaption(msg.getMessage("ServiceEditorBase.name"));
		name.setReadOnly(editMode);
		binder.forField(name).asRequired().bind("name");
		main.addComponent(name);

		Label binding = new Label();
		binding.setCaption(msg.getMessage("ServiceEditorBase.binding"));
		binding.setValue(ServiceTypeInfoHelper.getBinding(msg, type.getSupportedBinding()));
		main.addComponent(binding);
		if (type.getPaths() != null && !type.getPaths().isEmpty())
		{
			ChipsWithTextfield paths = new ChipsWithTextfield(msg);
			paths.setCaption(msg.getMessage("ServiceEditorBase.paths"));
			List<String> pathsList = type.getPaths().keySet().stream().collect(Collectors.toList());
			pathsList.set(0, pathsList.get(0) + " (" + msg.getMessage("default") + ")");
			paths.setValue(pathsList);
			paths.setReadOnly(true);
			main.addComponent(paths);
		}
		TextField contextPath = new TextField();
		contextPath.setCaption(msg.getMessage("ServiceEditorBase.contextPath"));
		contextPath.setReadOnly(editMode);
		binder.forField(contextPath).asRequired().withValidator((v, c) -> {
			try
			{
				EndpointPathValidator.validateEndpointPath(v);
			} catch (WrongArgumentException e)
			{
				return ValidationResult.error(msg.getMessage("ServiceEditorBase.invalidContextPath"));
			}

			return ValidationResult.ok();

		}).bind("address");
		main.addComponent(contextPath);

		I18nTextField displayedName = new I18nTextField(msg);
		displayedName.setCaption(msg.getMessage("ServiceEditorBase.displayedName"));
		binder.forField(displayedName).bind("displayedName");
		main.addComponent(displayedName);

		TextField description = new TextField();
		description.setCaption(msg.getMessage("ServiceEditorBase.description"));
		binder.forField(description).bind("description");
		main.addComponent(description);

		generalLayout.addComponent(main);
		return generalLayout;
	}

	private VerticalLayout buildAuthenticationTab()
	{
		authenticationLayout = new VerticalLayout();
		authenticationLayout.setMargin(false);
		authenticationLayout.setSpacing(false);
		mainAuthenticationLayout = new FormLayoutWithFixedCaptionWidth();

		ComboBox<String> realm = new ComboBox<>();
		realm.setCaption(msg.getMessage("ServiceEditorBase.realm"));
		realm.setItems(allRealms);
		realm.setEmptySelectionAllowed(false);
		binder.forField(realm).asRequired().bind("realm");
		mainAuthenticationLayout.addComponent(realm);

		Map<String, List<String>> labels = new HashMap<>();
		labels.put(msg.getMessage("ServiceEditorBase.flows"), flows);
		labels.put(msg.getMessage("ServiceEditorBase.authenticators"), authenticators);
		authAndFlows = new GroupedValuesChipsWithDropdown(labels);
		authAndFlows.setCaption(msg.getMessage("ServiceEditorBase.authenticatorsAndFlows"));
		binder.forField(authAndFlows).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}

			return ValidationResult.ok();

		}).bind("authenticationOptions");
		mainAuthenticationLayout.addComponent(authAndFlows);

		authenticationLayout.addComponent(mainAuthenticationLayout);

		return authenticationLayout;
	}
}
