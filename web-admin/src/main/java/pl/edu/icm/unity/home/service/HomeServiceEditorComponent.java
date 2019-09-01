/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.home.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.home.HomeEndpointProperties.RemovalModes;
import pl.edu.icm.unity.home.UserHomeEndpointFactory;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.authn.services.authnlayout.ServiceWebConfiguration;
import pl.edu.icm.unity.webui.authn.services.tabs.GeneralTab;
import pl.edu.icm.unity.webui.authn.services.tabs.WebServiceAuthenticationTab;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GridWithEditor;
import pl.edu.icm.unity.webui.common.ThemeConstans;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;
import pl.edu.icm.unity.webui.common.groups.MandatoryGroupSelection;

/**
 * Home service editor component. 
 * 
 * @author P.Piernik
 *
 */
class HomeServiceEditorComponent extends ServiceEditorBase
{
	private Binder<HomeServiceConfiguration> homeBinder;
	private String extraTab;
	private List<String> allAttributes;
	private List<Group> allGroups;
	private List<String> upManServices;
	private List<String> enquiryForms;
	private Binder<DefaultServiceDefinition> serviceBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private FileStorageService fileStorageService;

	HomeServiceEditorComponent(UnityMessageSource msg, URIAccessService uriAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			DefaultServiceDefinition toEdit, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			String extraTab, List<String> allAttributes, List<Group> allGroups, List<String> upManServices,
			List<String> enquiryForms, List<String> registrationForms, List<String> usedPaths,
			AuthenticatorSupportService authenticatorSupportService)
	{
		super(msg);
		this.fileStorageService = fileStorageService;
		this.extraTab = extraTab;
		this.allAttributes = allAttributes;
		this.allGroups = allGroups;
		this.upManServices = upManServices;
		this.enquiryForms = enquiryForms;

		boolean editMode = toEdit != null;
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);
		homeBinder = new Binder<>(HomeServiceConfiguration.class);
		webConfigBinder = new Binder<>(ServiceWebConfiguration.class);
		registerTab(new HomeGeneralTab(msg, serviceBinder, UserHomeEndpointFactory.TYPE, usedPaths, editMode));
		registerTab(new WebServiceAuthenticationTab(msg, uriAccessService, serverConfig,
				authenticatorSupportService, flows, authenticators, allRealms, registrationForms,
				UserHomeEndpointFactory.TYPE.getSupportedBinding(), serviceBinder, webConfigBinder));
		serviceBinder.setBean(editMode ? toEdit
				: new DefaultServiceDefinition(UserHomeEndpointFactory.TYPE.getName()));
		HomeServiceConfiguration config = new HomeServiceConfiguration();
		ServiceWebConfiguration webConfig = new ServiceWebConfiguration(ThemeConstans.unityTheme);
		if (editMode && toEdit.getConfiguration() != null)
		{
			config.fromProperties(toEdit.getConfiguration(), msg, extraTab, allGroups);
			webConfig.fromProperties(toEdit.getConfiguration(), msg, uriAccessService);
		}
		homeBinder.setBean(config);
		webConfigBinder.setBean(webConfig);
	}

	public static List<String> getAvailableTabs()
	{
		return new ArrayList<String>(Arrays.asList(HomeEndpointProperties.Components.credentialTab.toString(),
				HomeEndpointProperties.Components.preferencesTab.toString(),
				HomeEndpointProperties.Components.userDetailsTab.toString(),
				HomeEndpointProperties.Components.accountUpdateTab.toString()));
	}

	public static List<String> getAvailableControls()
	{
		return new ArrayList<String>(
				Arrays.asList(HomeEndpointProperties.Components.attributesManagement.toString(),
						HomeEndpointProperties.Components.identitiesManagement.toString(),
						HomeEndpointProperties.Components.accountRemoval.toString(),
						HomeEndpointProperties.Components.accountLinking.toString()));
	}

	public ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = serviceBinder.validate().hasErrors();
		hasErrors |= homeBinder.validate().hasErrors();
		hasErrors |= webConfigBinder.validate().hasErrors();
		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition service = serviceBinder.getBean();
		service.setConfiguration(homeBinder.getBean().toProperties(extraTab) + "\n"
				+ webConfigBinder.getBean().toProperties(msg, fileStorageService, service.getName()));
		return service;
	}

	public class HomeGeneralTab extends GeneralTab
	{
		private ChipsWithDropdown<String> enabledControls;
		private CheckBox allowRemovalSheduling;

		public HomeGeneralTab(UnityMessageSource msg, Binder<DefaultServiceDefinition> binder,
				EndpointTypeDescription type, List<String> usedPaths, boolean editMode)
		{
			super(msg, binder, type, usedPaths, editMode);
			mainLayout.addComponent(buildContentSection());
		}

		private Component buildContentSection()
		{
			FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
			main.setMargin(false);

			ChipsWithDropdown<String> enabledTabs = new ChipsWithDropdown<>();
			enabledTabs.setCaption(msg.getMessage("HomeServiceEditorComponent.enabledSections"));
			enabledTabs.setItems(getAvailableTabs());
			homeBinder.forField(enabledTabs).bind("enabledTabs");
			main.addComponent(enabledTabs);

			enabledControls = new ChipsWithDropdown<>();
			enabledControls.setCaption(
					msg.getMessage("HomeServiceEditorComponent.enabledUserDetailsControls"));
			List<String> enabledUserDetailsControls = getAvailableControls();
			enabledUserDetailsControls.add(extraTab);
			enabledControls.setItems(enabledUserDetailsControls);
			homeBinder.forField(enabledControls).bind("enabledUserDetailsControls");
			main.addComponent(enabledControls);

			GridWithEditor<ExposedAttribute> exposedAttributes = new GridWithEditor<>(msg,
					ExposedAttribute.class);
			exposedAttributes.setEnabled(false);
			exposedAttributes.setCaption(msg.getMessage("HomeServiceEditorComponent.exposedAttributes"));
			exposedAttributes.addComboColumn(s -> s.getName(), (t, v) -> t.setName(v),
					msg.getMessage("HomeServiceEditorComponent.attribute"), allAttributes, 10,
					false);

			exposedAttributes.addCheckBoxColumn(s -> s.isEditable(), (t, v) -> t.setEditable(v),
					msg.getMessage("HomeServiceEditorComponent.attributeEditable"), 10);

			exposedAttributes.addCheckBoxColumn(s -> s.isShowGroup(), (t, v) -> t.setShowGroup(v),
					msg.getMessage("HomeServiceEditorComponent.attributeShowGroup"), 10);

			MandatoryGroupSelection groupCombo = new MandatoryGroupSelection(msg);
			groupCombo.setItems(allGroups);

			exposedAttributes.addCustomColumn(s -> s.getGroup(),
					g -> g.group.getDisplayedName().getValue(msg), (t, v) -> t.setGroup(v),
					groupCombo, msg.getMessage("HomeServiceEditorComponent.attributeGroup"), 20);

			exposedAttributes.setWidth(100, Unit.PERCENTAGE);
			homeBinder.forField(exposedAttributes).bind("exposedAttributes");
			main.addComponent(exposedAttributes);

			enabledTabs.addValueChangeListener(e -> {
				boolean userDetTabEnabled = e.getValue()
						.contains(HomeEndpointProperties.Components.userDetailsTab.toString());
				exposedAttributes.setEnabled(userDetTabEnabled);
				enabledControls.setEnabled(userDetTabEnabled);
			});

			allowRemovalSheduling = new CheckBox();
			allowRemovalSheduling.setEnabled(false);
			allowRemovalSheduling.setCaption(
					msg.getMessage("HomeServiceEditorComponent.enableSelfRemovalScheduling"));
			homeBinder.forField(allowRemovalSheduling).bind("allowRemovalSheduling");
			main.addComponent(allowRemovalSheduling);

			ComboBox<RemovalModes> removalMode = new ComboBox<>();
			removalMode.setEnabled(false);
			removalMode.setCaption(msg.getMessage("HomeServiceEditorComponent.removalMode"));
			removalMode.setItems(RemovalModes.values());
			removalMode.setEmptySelectionAllowed(false);
			homeBinder.forField(removalMode).asRequired((v, c) -> {
				if (allowRemovalSheduling.getValue() && (v == null))
					return ValidationResult.error(msg.getMessage("fieldRequired"));
				return ValidationResult.ok();
			}).bind("removalMode");
			main.addComponent(removalMode);

			enabledControls.addValueChangeListener(e -> {
				boolean userAccountRemovalEnabled = e.getValue()
						.contains(HomeEndpointProperties.Components.accountRemoval.toString());
				allowRemovalSheduling.setEnabled(userAccountRemovalEnabled);
				removalMode.setEnabled(isAccountRemovalEnabled());
			});

			allowRemovalSheduling.addValueChangeListener(
					e -> removalMode.setEnabled(isAccountRemovalEnabled() && e.getValue()));

			CheckBox enableUpMan = new CheckBox();

			enableUpMan.setCaption(msg.getMessage("HomeServiceEditorComponent.enableUpMan"));
			homeBinder.forField(enableUpMan).bind("enableUpMan");
			main.addComponent(enableUpMan);

			ComboBox<String> upmanService = new ComboBox<>();
			upmanService.setEnabled(false);
			upmanService.setCaption(msg.getMessage("HomeServiceEditorComponent.upmanService"));
			upmanService.setItems(upManServices);
			upmanService.setEmptySelectionAllowed(false);
			homeBinder.forField(upmanService).bind("upManService");
			main.addComponent(upmanService);

			enableUpMan.addValueChangeListener(e -> {
				upmanService.setEnabled(e.getValue());
			});

			ChipsWithDropdown<String> enquiryFormsCombo = new ChipsWithDropdown<>();
			enquiryFormsCombo.setEnabled(false);
			enquiryFormsCombo.setCaption(msg.getMessage("HomeServiceEditorComponent.enquiryForms"));
			enquiryFormsCombo.setItems(enquiryForms);
			homeBinder.forField(enquiryFormsCombo).bind("enquiryForms");
			main.addComponent(enquiryFormsCombo);

			enabledTabs.addValueChangeListener(e -> {
				boolean accountUpdateEnabled = e.getValue().contains(
						HomeEndpointProperties.Components.accountUpdateTab.toString());
				enquiryFormsCombo.setEnabled(accountUpdateEnabled);
			});

			CollapsibleLayout contentSection = new CollapsibleLayout(
					msg.getMessage("HomeServiceEditorComponent.content"), main);
			contentSection.expand();
			return contentSection;
		}

		private boolean isAccountRemovalEnabled()
		{
			return enabledControls.getValue()
					.contains(HomeEndpointProperties.Components.accountRemoval.toString())
					&& allowRemovalSheduling.getValue();
		}

	}

	public static class ExposedAttribute
	{
		private String name;
		private GroupWithIndentIndicator group;
		private boolean editable;
		private boolean showGroup;

		public ExposedAttribute()
		{
			group = new GroupWithIndentIndicator(new Group("/"), false);
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public GroupWithIndentIndicator getGroup()
		{
			return group;
		}

		public void setGroup(GroupWithIndentIndicator group)
		{
			this.group = group;
		}

		public boolean isEditable()
		{
			return editable;
		}

		public void setEditable(boolean editable)
		{
			this.editable = editable;
		}

		public boolean isShowGroup()
		{
			return showGroup;
		}

		public void setShowGroup(boolean showGroup)
		{
			this.showGroup = showGroup;
		}
	}

	public class HomeServiceConfiguration
	{
		private List<String> enabledTabs;
		private List<String> enabledUserDetailsControls;
		private List<ExposedAttribute> exposedAttributes;
		private boolean allowRemovalSheduling;
		private RemovalModes removalMode;
		private boolean enableUpMan;
		private String upManService;
		private List<String> enquiryForms;

		public HomeServiceConfiguration()
		{
			enabledTabs = new ArrayList<>();
			enabledUserDetailsControls = new ArrayList<>();
			exposedAttributes = new ArrayList<>();
			allowRemovalSheduling = true;
			removalMode = RemovalModes.remove;
			enquiryForms = new ArrayList<>();
		}

		public String toProperties(String extraTab)
		{
			Properties raw = new Properties();
			List<String> allComponents = Stream.of(HomeEndpointProperties.Components.values())
					.map(HomeEndpointProperties.Components::toString).collect(Collectors.toList());
			allComponents.add(extraTab);

			allComponents.removeAll(enabledTabs);
			allComponents.removeAll(enabledUserDetailsControls);

			if (!allComponents.isEmpty())
			{
				allComponents.forEach(c -> raw.put(HomeEndpointProperties.PREFIX
						+ HomeEndpointProperties.DISABLED_COMPONENTS
						+ (allComponents.indexOf(c) + 1), c));
			}

			for (ExposedAttribute a : exposedAttributes)
			{
				raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.ATTRIBUTES
						+ (exposedAttributes.indexOf(a) + 1) + "."
						+ HomeEndpointProperties.GWA_ATTRIBUTE, a.getName());
				raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.ATTRIBUTES
						+ (exposedAttributes.indexOf(a) + 1) + "."
						+ HomeEndpointProperties.GWA_GROUP, a.getGroup().group.toString());

				raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.ATTRIBUTES
						+ (exposedAttributes.indexOf(a) + 1) + "."
						+ HomeEndpointProperties.GWA_EDITABLE, String.valueOf(a.isEditable()));

				raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.ATTRIBUTES
						+ (exposedAttributes.indexOf(a) + 1) + "."
						+ HomeEndpointProperties.GWA_SHOW_GROUP,
						String.valueOf(a.isShowGroup()));
			}

			raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.DISABLE_REMOVAL_SCHEDULE,
					String.valueOf(!allowRemovalSheduling));
			if (allowRemovalSheduling)
			{
				raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.REMOVAL_MODE,
						String.valueOf(removalMode));
			}
			raw.put(HomeEndpointProperties.PREFIX + HomeEndpointProperties.ENABLE_PROJECT_MANAGEMENT_LINK,
					String.valueOf(enableUpMan));
			if (enableUpMan)
			{
				if (upManService != null && !upManService.isEmpty())
				{
					raw.put(HomeEndpointProperties.PREFIX
							+ HomeEndpointProperties.PROJECT_MANAGEMENT_ENDPOINT,
							upManService);
				}
			}

			if (!enquiryForms.isEmpty())
			{
				enquiryForms.forEach(c -> raw.put(HomeEndpointProperties.PREFIX
						+ HomeEndpointProperties.ENQUIRIES + (enquiryForms.indexOf(c) + 1), c));
			}

			HomeEndpointProperties prop = new HomeEndpointProperties(raw);
			return prop.getAsString();
		}

		public void fromProperties(String properties, UnityMessageSource msg, String extraTab,
				List<Group> allGroups)
		{
			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(properties));
			} catch (IOException e)
			{
				throw new InternalException("Invalid configuration of the home ui service", e);
			}

			HomeEndpointProperties homeProperties = new HomeEndpointProperties(raw);

			List<String> disabledComponents = homeProperties
					.getListOfValues(HomeEndpointProperties.DISABLED_COMPONENTS);

			enabledTabs = getAvailableTabs();
			enabledUserDetailsControls = getAvailableControls();
			enabledUserDetailsControls.add(extraTab);

			enabledTabs.removeAll(disabledComponents);
			enabledUserDetailsControls.removeAll(disabledComponents);

			Set<String> attrKeys = homeProperties.getStructuredListKeys(HomeEndpointProperties.ATTRIBUTES);
			for (String key : attrKeys)
			{
				ExposedAttribute attr = new ExposedAttribute();
				attr.setName(homeProperties.getValue(key + HomeEndpointProperties.GWA_ATTRIBUTE));

				String groupPath = homeProperties.getValue(key + HomeEndpointProperties.GWA_GROUP);
				attr.setGroup(new GroupWithIndentIndicator(
						allGroups.stream().filter(g -> g.toString().equals(groupPath))
								.findFirst().orElse(new Group(groupPath)),
						false));
				attr.setEditable(homeProperties
						.getBooleanValue(key + HomeEndpointProperties.GWA_EDITABLE));
				attr.setShowGroup(homeProperties
						.getBooleanValue(key + HomeEndpointProperties.GWA_SHOW_GROUP));
				exposedAttributes.add(attr);
			}

			removalMode = homeProperties.getEnumValue(HomeEndpointProperties.REMOVAL_MODE,
					HomeEndpointProperties.RemovalModes.class);
			allowRemovalSheduling = !homeProperties
					.getBooleanValue(HomeEndpointProperties.DISABLE_REMOVAL_SCHEDULE);

			enableUpMan = homeProperties
					.getBooleanValue(HomeEndpointProperties.ENABLE_PROJECT_MANAGEMENT_LINK);
			upManService = homeProperties.getValue(HomeEndpointProperties.PROJECT_MANAGEMENT_ENDPOINT);

			enquiryForms = homeProperties.getListOfValues(HomeEndpointProperties.ENQUIRIES);
		}

		public List<String> getEnabledTabs()
		{
			return enabledTabs;
		}

		public void setEnabledTabs(List<String> enabledTabs)
		{
			this.enabledTabs = enabledTabs;
		}

		public List<String> getEnabledUserDetailsControls()
		{
			return enabledUserDetailsControls;
		}

		public void setEnabledUserDetailsControls(List<String> enabledUserDetailsControls)
		{
			this.enabledUserDetailsControls = enabledUserDetailsControls;
		}

		public List<ExposedAttribute> getExposedAttributes()
		{
			return exposedAttributes;
		}

		public void setExposedAttributes(List<ExposedAttribute> exposedAttributes)
		{
			this.exposedAttributes = exposedAttributes;
		}

		public boolean isAllowRemovalSheduling()
		{
			return allowRemovalSheduling;
		}

		public void setAllowRemovalSheduling(boolean allowRemovalSheduling)
		{
			this.allowRemovalSheduling = allowRemovalSheduling;
		}

		public RemovalModes getRemovalMode()
		{
			return removalMode;
		}

		public void setRemovalMode(RemovalModes removalMode)
		{
			this.removalMode = removalMode;
		}

		public boolean isEnableUpMan()
		{
			return enableUpMan;
		}

		public void setEnableUpMan(boolean enableUpMan)
		{
			this.enableUpMan = enableUpMan;
		}

		public String getUpManService()
		{
			return upManService;
		}

		public void setUpManService(String upManService)
		{
			this.upManService = upManService;
		}

		public List<String> getEnquiryForms()
		{
			return enquiryForms;
		}

		public void setEnquiryForms(List<String> enquiryForms)
		{
			this.enquiryForms = enquiryForms;
		}

	}

}