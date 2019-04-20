/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.web.authnEditor;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
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
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Image;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.client.OAuth2Verificator;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
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
	PKIManagement pkiMan;
	private InputTranslationProfileFieldFactory profileFieldFactory;
	private RegistrationsManagement registrationMan;
	private ProvidersComponent providersComponent;
	private Binder<OAuthConfiguration> configBinder;
	private SubViewSwitcher subViewSwitcher;

	public OAuthAuthenticatorEditor(UnityMessageSource msg, PKIManagement pkiMan,
			InputTranslationProfileFieldFactory profileFieldFactory, RegistrationsManagement registrationMan)
	{
		super(msg);
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
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

		providersComponent = new ProvidersComponent();
		providersComponent.setCaption(msg.getMessage("OAuthAuthenticatorEditor.providers"));
		configBinder.forField(providersComponent).bind("providers");
		header.addComponent(providersComponent);
		
		OAuthConfiguration config = new OAuthConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg, pkiMan);
		}

		configBinder.setBean(config);
		

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

		List<OAuthProviderConfiguration> providersConfigs = configBinder.getBean().getProviders();

		if (providersConfigs.isEmpty())
		{
			providersComponent.setComponentError(
					new UserError(msg.getMessage("OAuthAuthenticatorEditor.emptyProvidersError")));
			throw new FormValidationException();
		}

		OAuthConfiguration config = configBinder.getBean();
		try
		{
			return config.toProperties(msg, pkiMan);
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the oauth2 verificator", e);
		}
	}

	private class ProvidersComponent extends CustomField<List<OAuthProviderConfiguration>>
	{
		private GridWithActionColumn<OAuthProviderConfiguration> providersList;
		private VerticalLayout main;

		public ProvidersComponent()
		{
			initUI();
		}

		private void initUI()
		{
			main = new VerticalLayout();
			main.setMargin(false);

			Button add = new Button(msg.getMessage("ProvidersComponent.addProvider"));
			add.addClickListener(e -> {
				setComponentError(null);
				gotoEditSubView(null, providersList.getElements().stream().map(p -> p.getId())
						.collect(Collectors.toSet()), c -> {
							subViewSwitcher.exitSubView();
							providersList.addElement(c);
							fireChange();
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
		}

		private Image getLogo(String logoUrl)
		{
			Resource logo;
			try
			{
				logo = logoUrl == null || logoUrl.isEmpty() ? Images.empty.getResource()
						: ImageUtils.getLogoResource(logoUrl);
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
									providersList.replaceElement(edited, c);
									fireChange();
									subViewSwitcher.exitSubView();
								});
					}

					).build();

			SingleActionHandler<OAuthProviderConfiguration> remove = SingleActionHandler
					.builder4Delete(msg, OAuthProviderConfiguration.class).withHandler(r -> {
						providersList.removeElement(r.iterator().next());
						fireChange();
					}).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoEditSubView(OAuthProviderConfiguration edited, Set<String> usedIds,
				Consumer<OAuthProviderConfiguration> onConfirm)
		{
			Set<String> forms;
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

			EditOAuthProviderSubView subView = new EditOAuthProviderSubView(msg, pkiMan, profileFieldFactory,
					edited, usedIds, subViewSwitcher, forms, validators, r -> {
						onConfirm.accept(r);
						name.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						name.focus();
					});
			subViewSwitcher.goToSubView(subView);
		}

		private Set<String> getRegistrationForms() throws EngineException
		{
			return registrationMan.getForms().stream().map(r -> r.getName()).collect(Collectors.toSet());
		}

		@Override
		public List<OAuthProviderConfiguration> getValue()
		{
			return providersList.getElements();
		}

		@Override
		protected Component initContent()
		{
			return main;
		}

		@Override
		protected void doSetValue(List<OAuthProviderConfiguration> value)
		{
			providersList.setItems(value);

		}

		private void fireChange()
		{
			fireEvent(new ValueChangeEvent<List<OAuthProviderConfiguration>>(this,
					providersList.getElements(), true));
		}

	}
}
