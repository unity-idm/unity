/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.console;

import java.net.URL;
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
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.webelements.clipboard.CopyToClipboardButton;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.client.OAuth2Verificator;
import pl.edu.icm.unity.oauth.client.ResponseConsumerServlet;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FileStreamResource;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

class OAuthAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private PKIManagement pkiMan;
	private FileStorageService fileStorageService;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfig;
	private InputTranslationProfileFieldFactory profileFieldFactory;
	private RegistrationsManagement registrationMan;
	private ProvidersComponent providersComponent;
	private Binder<OAuthConfiguration> configBinder;
	private SubViewSwitcher subViewSwitcher;
	private AdvertisedAddressProvider advertisedAddrProvider;
	private ImageAccessService imageAccessService;

	OAuthAuthenticatorEditor(MessageSource msg,
			UnityServerConfiguration serverConfig,
			PKIManagement pkiMan,
			FileStorageService fileStorageService,
			URIAccessService uriAccessService,
			ImageAccessService imageAccessService,
			InputTranslationProfileFieldFactory profileFieldFactory,
			RegistrationsManagement registrationMan,
			AdvertisedAddressProvider advertisedAddrProvider)
	{
		super(msg);
		this.pkiMan = pkiMan;
		this.imageAccessService = imageAccessService;
		this.profileFieldFactory = profileFieldFactory;
		this.registrationMan = registrationMan;
		this.fileStorageService = fileStorageService;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
		this.advertisedAddrProvider = advertisedAddrProvider;
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
		
		TextField returnURLInfo = new TextField();
		returnURLInfo.setValue(buildReturnURL());
		returnURLInfo.setReadOnly(true);
		returnURLInfo.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH,
				FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		CopyToClipboardButton copy = new CopyToClipboardButton(msg, returnURLInfo);
		HorizontalLayout hr = new HorizontalLayout(returnURLInfo, copy);
		hr.setComponentAlignment(copy, Alignment.MIDDLE_LEFT);
		hr.setCaption(msg.getMessage("OAuthAuthenticatorEditor.returnURLInfo"));
		header.addComponent(hr);
		
		configBinder.forField(accountAssociation).bind("defAccountAssociation");

		providersComponent = new ProvidersComponent();
		providersComponent.setCaption(msg.getMessage("OAuthAuthenticatorEditor.providers"));
		configBinder.forField(providersComponent).bind("providers");
		header.addComponent(providersComponent);

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);

		OAuthConfiguration config = new OAuthConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg, pkiMan, imageAccessService);
		}

		configBinder.setBean(config);

		return mainView;
	}

	private String buildReturnURL()
	{
		URL serverURL = advertisedAddrProvider.get();
		return serverURL.toExternalForm() + SharedEndpointManagement.CONTEXT_PATH + ResponseConsumerServlet.PATH;
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
			return config.toProperties(msg, pkiMan, fileStorageService, getName());
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
				gotoNew();
			});
			add.setIcon(Images.add.getResource());
			main.addComponent(add);
			main.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);

			providersList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
			providersList.addComponentColumn(p -> getLogo(p.getLogo()),
					msg.getMessage("ProvidersComponent.logo"), 2);
			providersList.addComponentColumn(p -> StandardButtonsHelper
					.buildLinkButton(p.getName().getValue(msg), e -> gotoEdit(p)),
					msg.getMessage("ProvidersComponent.id"), 30);
			providersList.addColumn(p -> p.getName().getValue(msg),
					msg.getMessage("ProvidersComponent.name"), 50);

			main.addComponent(providersList);
		}

		private Image getLogo(LocalOrRemoteResource res)
		{
			Resource logo;
			try
			{
				logo = res == null ? Images.empty.getResource()
						: res.getLocal() != null
								? new FileStreamResource(res.getLocal()).getResource()
								: new FileStreamResource(uriAccessService.readImageURI(
										URIHelper.parseURI(res.getRemote()),
										UI.getCurrent().getTheme())
										.getContents()).getResource();
			} catch (Exception e)
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
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<OAuthProviderConfiguration> remove = SingleActionHandler
					.builder4Delete(msg, OAuthProviderConfiguration.class).withHandler(r -> {
						providersList.removeElement(r.iterator().next());
						fireChange();
					}).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoNew()
		{
			gotoEditSubView(null, providersList.getElements().stream().map(p -> p.getId())
					.collect(Collectors.toSet()), c -> {
						subViewSwitcher.exitSubViewAndShowUpdateInfo();
						providersList.addElement(c);
					});

		}

		private void gotoEdit(OAuthProviderConfiguration edited)
		{
			gotoEditSubView(edited,
					providersList.getElements().stream().filter(p -> p.getId() != edited.getId())
							.map(p -> p.getId()).collect(Collectors.toSet()),
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
				NotificationPopup.showError(msg, "Can not init OAuth provider editor", e);
				return;
			}

			EditOAuthProviderSubView subView = new EditOAuthProviderSubView(msg, serverConfig, pkiMan,
					uriAccessService, imageAccessService, profileFieldFactory, edited, usedIds, subViewSwitcher, forms,
					validators, r -> {
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
