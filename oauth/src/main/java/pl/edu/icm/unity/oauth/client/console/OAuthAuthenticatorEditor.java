/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.console;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.elements.CopyToClipboardButton;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.oauth.client.OAuth2Verificator;
import pl.edu.icm.unity.oauth.client.ResponseConsumerServlet;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.*;

class OAuthAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private final PKIManagement pkiMan;
	private final FileStorageService fileStorageService;
	private final InputTranslationProfileFieldFactory profileFieldFactory;
	private final RegistrationsManagement registrationMan;
	private ProvidersComponent providersComponent;
	private Binder<OAuthConfiguration> configBinder;
	private SubViewSwitcher subViewSwitcher;
	private final AdvertisedAddressProvider advertisedAddrProvider;
	private final VaadinLogoImageLoader imageAccessService;
	private final UnityServerConfiguration serverConfig;
	private final NotificationPresenter notificationPresenter;

	OAuthAuthenticatorEditor(MessageSource msg,
			PKIManagement pkiMan,
			FileStorageService fileStorageService,
			VaadinLogoImageLoader imageAccessService,
			InputTranslationProfileFieldFactory profileFieldFactory,
			RegistrationsManagement registrationMan,
			AdvertisedAddressProvider advertisedAddrProvider,
			UnityServerConfiguration serverConfig,
			NotificationPresenter notificationPresenter)
	{
		super(msg);
		this.pkiMan = pkiMan;
		this.imageAccessService = imageAccessService;
		this.profileFieldFactory = profileFieldFactory;
		this.registrationMan = registrationMan;
		this.fileStorageService = fileStorageService;
		this.advertisedAddrProvider = advertisedAddrProvider;
		this.serverConfig = serverConfig;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		this.subViewSwitcher = subViewSwitcher;

		boolean editMode = init(msg.getMessage("OAuthAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(OAuthConfiguration.class);

		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));
		Checkbox accountAssociation = new Checkbox(
				msg.getMessage("OAuthAuthenticatorEditor.accountAssociation"));
		header.addFormItem(accountAssociation, "");
		
		TextField returnURLInfo = new TextField();
		returnURLInfo.setValue(buildReturnURL());
		returnURLInfo.setReadOnly(true);
		returnURLInfo.setWidth(TEXT_FIELD_BIG.value());
		CopyToClipboardButton copy = new CopyToClipboardButton(msg::getMessage, returnURLInfo);
		HorizontalLayout field = new HorizontalLayout(returnURLInfo, copy);
		field.setAlignItems(FlexComponent.Alignment.CENTER);
		field.addClassName(SMALL_GAP.getName());
		header.addFormItem(field, msg.getMessage("OAuthAuthenticatorEditor.returnURLInfo"));
		
		configBinder.forField(accountAssociation).bind("defAccountAssociation");

		providersComponent = new ProvidersComponent();
		configBinder.forField(providersComponent).bind("providers");
		header.addFormItem(providersComponent, msg.getMessage("OAuthAuthenticatorEditor.providers"));

		VerticalLayout mainView = new VerticalLayout();
		mainView.setPadding(false);
		mainView.add(header);

		OAuthConfiguration config = new OAuthConfiguration();
		if (editMode)
			config.fromProperties(toEdit.configuration, msg, pkiMan, imageAccessService);

		configBinder.setBean(config);

		return mainView;
	}

	private String buildReturnURL()
	{
		URL serverURL = advertisedAddrProvider.get();
		return serverURL.toExternalForm() + SharedEndpointManagement.CONTEXT_PATH + ResponseConsumerServlet.PATH;
	}
	
	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
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
			providersComponent.setErrorMessage(msg.getMessage("OAuthAuthenticatorEditor.emptyProvidersError"));
			throw new FormValidationException();
		}

		OAuthConfiguration config = configBinder.getBean();
		try
		{
			return config.toProperties(msg, pkiMan, fileStorageService, getName());
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the oauth2 verificator", e);
		}
	}

	private class ProvidersComponent extends CustomField<List<OAuthProviderConfiguration>>
	{
		private GridWithActionColumn<OAuthProviderConfiguration> providersList;
		private GridListDataView<OAuthProviderConfiguration> dataView;

		public ProvidersComponent()
		{
			initUI();
		}

		@Override
		protected List<OAuthProviderConfiguration> generateModelValue()
		{
			return dataView.getItems().toList();
		}

		@Override
		protected void setPresentationValue(List<OAuthProviderConfiguration> oAuthProviderConfigurations)
		{
			dataView = providersList.setItems(oAuthProviderConfigurations);
		}

		private void initUI()
		{
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);
			main.setAlignItems(FlexComponent.Alignment.END);

			Button add = new Button(msg.getMessage("ProvidersComponent.addProvider"));
			add.addClickListener(e -> gotoNew());
			add.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
			main.add(add);

			providersList = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
			providersList.addComponentColumn(oAuthProviderConfiguration ->
					{
						Image logo = oAuthProviderConfiguration.getLogo();
						logo.setClassName(LOGO_GRID_IMAGE.getName());
						return logo;
					})
					.setHeader(msg.getMessage("ProvidersComponent.logo"))
					.setAutoWidth(true);
			providersList.addComponentColumn(p -> new LinkButton(p.getName().getValue(msg), e -> gotoEdit(p)))
					.setHeader(msg.getMessage("ProvidersComponent.id"))
					.setAutoWidth(true);
			providersList.addColumn(p -> p.getName().getValue(msg))
					.setHeader(msg.getMessage("ProvidersComponent.name"))
					.setAutoWidth(true);

			setWidth("40em");
			setHeight("23em");
			providersList.setHeight("17em");
			main.add(providersList);
			add(main);
		}

		private List<SingleActionHandler<OAuthProviderConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<OAuthProviderConfiguration> edit = SingleActionHandler
					.builder4Edit(msg::getMessage, OAuthProviderConfiguration.class).withHandler(r -> {
						OAuthProviderConfiguration edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<OAuthProviderConfiguration> remove = SingleActionHandler
					.builder4Delete(msg::getMessage, OAuthProviderConfiguration.class).withHandler(r -> {
						providersList.removeElement(r.iterator().next());
						fireChange();
					}).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoNew()
		{
			gotoEditSubView(null, providersList.getElements().stream().map(OAuthProviderConfiguration::getId)
					.collect(Collectors.toSet()), c -> {
						subViewSwitcher.exitSubViewAndShowUpdateInfo();
						providersList.addElement(c);
					});

		}

		private void gotoEdit(OAuthProviderConfiguration edited)
		{
			gotoEditSubView(edited,
					providersList.getElements().stream().map(OAuthProviderConfiguration::getId)
							.filter(id -> !Objects.equals(id, edited.getId())).collect(Collectors.toSet()),
					c -> {
						providersList.replaceElement(edited, c);
						subViewSwitcher.exitSubViewAndShowUpdateInfo();
					});
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
				notificationPresenter.showError("Can not init OAuth provider editor", e.getMessage());
				return;
			}

			EditOAuthProviderSubView subView = new EditOAuthProviderSubView(msg, pkiMan, notificationPresenter,
					imageAccessService, profileFieldFactory, edited, usedIds, subViewSwitcher, forms,
					validators, serverConfig, r -> {
						onConfirm.accept(r);
						fireChange();
						providersList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						providersList.focus();
					});
			subViewSwitcher.goToSubView(subView);
		}

		private Set<String> getRegistrationForms() throws EngineException
		{
			return registrationMan.getForms().stream().map(DescribedObjectROImpl::getName).collect(Collectors.toSet());
		}

		@Override
		public List<OAuthProviderConfiguration> getValue()
		{
			return providersList.getElements();
		}

		private void fireChange()
		{
			fireEvent(new ComponentValueChangeEvent<>(this, this,
					providersList.getElements(), true));
		}

	}
}
