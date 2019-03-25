/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.web.authnEditor;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.utils.tprofile.EditInputTranslationProfileSubViewHelper;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.OAuth2Verificator;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.oauth.client.web.authnEditor.EditOAuthProviderSubView.OAuthProviderConfiguration;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * OAuth authenticator editor
 * 
 * @author P.Piernik
 *
 */
public class OAuthAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private PKIManagement pkiMan;
	private EditInputTranslationProfileSubViewHelper profileHelper;
	private RegistrationsManagement registrationMan;
	
	private ProvidersComponent providersComponent;
	private Binder<OAuthConfiguration> configBinder;
	private SubViewSwitcher subViewSwitcher;
	
	public OAuthAuthenticatorEditor(UnityMessageSource msg, PKIManagement pkiMan,
			EditInputTranslationProfileSubViewHelper profileHelper, RegistrationsManagement registrationMan)
	{
		super(msg);
		this.pkiMan = pkiMan;
		this.profileHelper = profileHelper;
		this.registrationMan = registrationMan;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		this.subViewSwitcher = subViewSwitcher;

		boolean editMode = init(msg.getMessage("OAuthAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(OAuthConfiguration.class);

		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);
		CheckBox accountAssociation = new CheckBox(
				msg.getMessage("OAuthAuthenticatorEditor.accountAssociation"));
		header.addComponent(accountAssociation);
		configBinder.forField(accountAssociation).bind("accountAssociation");

		OAuthConfiguration config = new OAuthConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}

		configBinder.setBean(config);

		providersComponent = new ProvidersComponent();
		providersComponent.setValue(configBinder.getBean().providers);
		providersComponent.setCaption(msg.getMessage("OAuthAuthenticatorEditor.providers"));

		header.addComponent(providersComponent);

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		return mainView;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), OAuth2Verificator.NAME, getConfiguration(), null);
	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();

		List<OAuthProviderConfiguration> providersConfigs = providersComponent.getConfigurations();

		if (providersConfigs.isEmpty())
		{
			providersComponent.setComponentError(
					new UserError(msg.getMessage("OAuthAuthenticatorEditor.emptyProvidersError")));
			throw new FormValidationException();
		}

		OAuthConfiguration config = configBinder.getBean();
		config.setProviders(providersComponent.getConfigurations());

		return config.toProperties();

	}

	public class OAuthConfiguration
	{
		private boolean accountAssociation;
		private List<OAuthProviderConfiguration> providers;

		public OAuthConfiguration()
		{
			providers = new ArrayList<>();
		}

		public void setProviders(List<OAuthProviderConfiguration> configurations)
		{
			providers.clear();
			providers.addAll(configurations);

		}

		public boolean isAccountAssociation()
		{
			return accountAssociation;
		}

		public void setAccountAssociation(boolean accountAssociation)
		{
			this.accountAssociation = accountAssociation;
		}

		public void fromProperties(String properties, UnityMessageSource msg)
		{
			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(properties));
			} catch (IOException e)
			{
				throw new InternalException("Invalid configuration of the oauth2 verificator", e);
			}

			OAuthClientProperties oauthProp = new OAuthClientProperties(raw, pkiMan);
			accountAssociation = oauthProp.getBooleanValue(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION);

			providers.clear();
			Set<String> keys = oauthProp.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
			for (String key : keys)
			{
				String idpKey = key.substring(OAuthClientProperties.PROVIDERS.length(),
						key.length() - 1);

				OAuthProviderConfiguration provider = new OAuthProviderConfiguration();
				CustomProviderProperties providerProps = oauthProp.getProvider(key);
				provider.fromProperties(msg, providerProps, idpKey);
				providers.add(provider);
			}

		}

		public String toProperties()
		{
			Properties raw = new Properties();

			raw.put(OAuthClientProperties.P + CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION,
					String.valueOf(accountAssociation));

			for (OAuthProviderConfiguration provider : providers)
			{
				provider.toProperties(raw, msg);
			}

			OAuthClientProperties prop = new OAuthClientProperties(raw, pkiMan);
			return prop.getAsString();
		}

	}

	private class ProvidersComponent extends CustomComponent
	{
		private GridWithActionColumn<OAuthProviderConfiguration> providersList;

		public ProvidersComponent()
		{
			initUI();
		}

		public void setValue(List<OAuthProviderConfiguration> providers)
		{
			for (OAuthProviderConfiguration config : providers)
			{
				providersList.addElement(config);
			}
		}

		private void initUI()
		{
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);

			Button add = new Button(msg.getMessage("ProvidersComponent.addProvider"));
			add.addClickListener(e -> {
				providersComponent.setComponentError(null);
				gotoEditSubView(null, providersList.getElements().stream().map(p -> p.getId())
						.collect(Collectors.toSet()), c -> {
							subViewSwitcher.exitSubView();
							providersList.addElement(c);
						});
			});
			add.setIcon(Images.add.getResource());
			main.addComponent(add);
			main.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);

			providersList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
			providersList.addComponentColumn(p -> getLogo(p.getIconUrl()),
					msg.getMessage("ProvidersComponent.logo"), 10);
			providersList.addColumn(p -> p.getId(), msg.getMessage("ProvidersComponent.id"), 10);
			providersList.addColumn(p -> p.getName().getValue(msg),
					msg.getMessage("ProvidersComponent.name"), 50);

			main.addComponent(providersList);
			setCompositionRoot(main);
		}

		private Image getLogo(I18nString logoUrl)
		{
			Resource logo;
			try
			{
				logo = logoUrl == null || logoUrl.isEmpty() ? Images.empty.getResource()
						: ImageUtils.getLogoResource(logoUrl.getValue(msg));
			} catch (MalformedURLException e)
			{
				logo = Images.error.getResource();
			}
			Image img = new Image("", logo);
			img.setHeight(25, Unit.PIXELS);
			return img;
		}

		private List<SingleActionHandler<OAuthProviderConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<OAuthProviderConfiguration> edit = SingleActionHandler
					.builder4Edit(msg, OAuthProviderConfiguration.class).withHandler(r -> {
						OAuthProviderConfiguration edited = r.iterator().next();
						gotoEditSubView(edited, providersList.getElements().stream()
								.filter(p -> p.getId() != edited.getId())
								.map(p -> p.getId()).collect(Collectors.toSet()), c -> {
									providersList.replaceEntry(edited, c);
									subViewSwitcher.exitSubView();
								});
					}

					).build();

			SingleActionHandler<OAuthProviderConfiguration> remove = SingleActionHandler
					.builder4Delete(msg, OAuthProviderConfiguration.class)
					.withHandler(r -> providersList.removeElement(r.iterator().next())).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoEditSubView(OAuthProviderConfiguration edited, Set<String> usedIds,
				Consumer<OAuthProviderConfiguration> onConfirm)
		{
			List<String> forms;
			Set<String> validators;

			try
			{
				validators = pkiMan.getValidatorNames();
				forms = getRegistrationForms();

			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, "", e);
				return;
			}

			EditOAuthProviderSubView subView = new EditOAuthProviderSubView(msg, pkiMan, profileHelper,
					edited, usedIds, subViewSwitcher, forms, validators, r -> {
						onConfirm.accept(r);
						name.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						name.focus();
					});
			subViewSwitcher.goToSubView(subView);
		}

		public List<OAuthProviderConfiguration> getConfigurations()
		{
			return providersList.getElements();
		}

		private List<String> getRegistrationForms() throws EngineException
		{
			return registrationMan.getForms().stream().map(r -> r.getName()).collect(Collectors.toList());
		}

	}
}
