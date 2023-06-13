/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.console;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.tooltip.TooltipExtension;
import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.saml.console.SAMLIdentityMapping;
import pl.edu.icm.unity.saml.sp.SAMLVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.GridWithEditor;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.chips.ChipsWithFreeText;
import pl.edu.icm.unity.webui.common.file.FileField;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorDocument;

/**
 * SAML Authenticator editor
 * 
 * @author P.Piernik
 *
 */
class SAMLAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	public static final List<String> STANDART_NAME_FORMATS = Arrays.asList(
			"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
			"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
			"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName",
			"urn:oasis:names:tc:SAML:2.0:nameid-format:transient");

	private PKIManagement pkiMan;
	private FileStorageService fileStorageService;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfig;
	private InputTranslationProfileFieldFactory profileFieldFactory;
	private RegistrationsManagement registrationMan;
	private Binder<SAMLAuthneticatorConfiguration> configBinder;
	private SubViewSwitcher subViewSwitcher;

	private Set<String> credentials;
	private Set<String> realms;
	private List<String> idTypes;

	private CheckBox defSignRequest;
	private IndividualTrustedIdpComponent idps;
	private CheckBox signMetadata;
	private ImageAccessService imageAccessService;

	SAMLAuthenticatorEditor(MessageSource msg, UnityServerConfiguration serverConfig, PKIManagement pkiMan,
			InputTranslationProfileFieldFactory profileFieldFactory,
			RegistrationsManagement registrationMan, RealmsManagement realmMan,
			IdentityTypesRegistry idTypesReg, FileStorageService fileStorageService,
			URIAccessService uriAccessService, ImageAccessService imageAccessService) throws EngineException
	{
		super(msg);
		this.fileStorageService = fileStorageService;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
		this.registrationMan = registrationMan;
		this.imageAccessService = imageAccessService;
		this.credentials = pkiMan.getCredentialNames();
		this.realms = realmMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toSet());
		this.idTypes = idTypesReg.getAll().stream().map(i -> i.getId()).collect(Collectors.toList());

	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		this.subViewSwitcher = subViewSwitcher;

		boolean editMode = init(msg.getMessage("SAMLAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(SAMLAuthneticatorConfiguration.class);

		FormLayoutWithFixedCaptionWidth header = buildHeaderSection();
		CollapsibleLayout trustedFederations = buildTrustedFederationsSection();
		trustedFederations.expand();
		CollapsibleLayout individualTrustedIdPs = buildIndividualTrustedIdPsSection();
		CollapsibleLayout metadataPublishing = buildSAMLMetadaPublishingSection();
		CollapsibleLayout singleLogout = buildSingleLogoutSection();

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(trustedFederations);
		mainView.addComponent(individualTrustedIdPs);
		mainView.addComponent(metadataPublishing);
		mainView.addComponent(singleLogout);

		SAMLAuthneticatorConfiguration config = new SAMLAuthneticatorConfiguration();
		if (editMode)
		{
			config.fromProperties(pkiMan, uriAccessService, imageAccessService, msg, toEdit.configuration);
		}

		configBinder.setBean(config);

		return mainView;
	}

	private FormLayoutWithFixedCaptionWidth buildHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);
		name.focus();

		TextField requesterId = new TextField(msg.getMessage("SAMLAuthenticatorEditor.requesterId"));
		requesterId.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(requesterId).asRequired(msg.getMessage("fieldRequired")).bind("requesterId");
		header.addComponent(requesterId);

		ComboBox<String> credential = new ComboBox<>();
		credential.setCaption(msg.getMessage("SAMLAuthenticatorEditor.credential"));
		credential.setItems(credentials);
		configBinder.forField(credential).asRequired(
				(v, c) -> ((v == null || v.isEmpty()) && (isSignReq() || signMetadata.getValue()))
						? ValidationResult.error(msg.getMessage("fieldRequired"))
						: ValidationResult.ok())
				.bind("credential");
		header.addComponent(credential);
		
		ComboBox<String> additionalCredential = new ComboBox<>();
		additionalCredential.setCaption(msg.getMessage("SAMLAuthenticatorEditor.additionalCredential"));
		additionalCredential.setDescription(msg.getMessage("SAMLAuthenticatorEditor.additionalCredentialDesc"));
		TooltipExtension.tooltip(additionalCredential);
		additionalCredential.setItems(credentials);
		configBinder.forField(additionalCredential)
				.bind("additionalCredential");
		header.addComponent(additionalCredential);

		CheckBox includeAddtionalCredentialInMetadata = new CheckBox(msg.getMessage("SAMLAuthenticatorEditor.includeAddtionalCredentialInMetadata"));
		configBinder.forField(includeAddtionalCredentialInMetadata).bind("includeAddtionalCredentialInMetadata");
		header.addComponent(includeAddtionalCredentialInMetadata);
			
		ChipsWithFreeText acceptedNameFormats = new ChipsWithFreeText(msg);
		acceptedNameFormats.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH,
				FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		acceptedNameFormats.setCaption(msg.getMessage("SAMLAuthenticatorEditor.acceptedNameFormats"));
		acceptedNameFormats.setItems(STANDART_NAME_FORMATS);
		header.addComponent(acceptedNameFormats);
		configBinder.forField(acceptedNameFormats).bind("acceptedNameFormats");

		CheckBox requireSignedAssertion = new CheckBox(
				msg.getMessage("SAMLAuthenticatorEditor.requireSignedAssertion"));
		configBinder.forField(requireSignedAssertion).bind("requireSignedAssertion");
		header.addComponent(requireSignedAssertion);

		defSignRequest = new CheckBox(msg.getMessage("SAMLAuthenticatorEditor.defSignRequest"));
		configBinder.forField(defSignRequest).bind("defSignRequest");
		header.addComponent(defSignRequest);

		ChipsWithFreeText defaultRequestedNameFormat = new ChipsWithFreeText(msg);
		defaultRequestedNameFormat.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH,
				FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		defaultRequestedNameFormat
				.setCaption(msg.getMessage("SAMLAuthenticatorEditor.defaultRequestedNameFormat"));
		defaultRequestedNameFormat.setItems(STANDART_NAME_FORMATS);
		defaultRequestedNameFormat.setMaxSelection(1);
		header.addComponent(defaultRequestedNameFormat);
		configBinder.forField(defaultRequestedNameFormat).bind("defaultRequestedNameFormat");

		CheckBox defAccountAssociation = new CheckBox(
				msg.getMessage("SAMLAuthenticatorEditor.defAccountAssociation"));
		configBinder.forField(defAccountAssociation).bind("defAccountAssociation");
		header.addComponent(defAccountAssociation);

		return header;
	}

	private CollapsibleLayout buildTrustedFederationsSection()
	{
		FormLayoutWithFixedCaptionWidth trustedFederations = new FormLayoutWithFixedCaptionWidth();
		trustedFederations.setMargin(false);
		TrustedFederationComponent federations = new TrustedFederationComponent();
		configBinder.forField(federations).bind("trustedFederations");
		trustedFederations.addComponent(federations);

		return new CollapsibleLayout(msg.getMessage("SAMLAuthenticatorEditor.trustedFederations"),
				trustedFederations);
	}

	private CollapsibleLayout buildIndividualTrustedIdPsSection()
	{
		FormLayoutWithFixedCaptionWidth individualTrustedIdPs = new FormLayoutWithFixedCaptionWidth();
		individualTrustedIdPs.setMargin(false);

		idps = new IndividualTrustedIdpComponent();
		configBinder.forField(idps).bind("individualTrustedIdps");
		individualTrustedIdPs.addComponent(idps);

		return new CollapsibleLayout(msg.getMessage("SAMLAuthenticatorEditor.individualTrustedIdPs"),
				individualTrustedIdPs);
	}

	private CollapsibleLayout buildSAMLMetadaPublishingSection()
	{
		FormLayoutWithFixedCaptionWidth metadataPublishing = new FormLayoutWithFixedCaptionWidth();
		metadataPublishing.setMargin(false);

		CheckBox publishMetadata = new CheckBox(msg.getMessage("SAMLAuthenticatorEditor.publishMetadata"));
		configBinder.forField(publishMetadata).bind("publishMetadata");
		metadataPublishing.addComponent(publishMetadata);

		TextField metadataPath = new TextField(msg.getMessage("SAMLAuthenticatorEditor.metadataPath"));
		metadataPath.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(metadataPath)
				.asRequired((v, c) -> ((v == null || v.isEmpty()) && publishMetadata.getValue())
						? ValidationResult.error(msg.getMessage("fieldRequired"))
						: ValidationResult.ok())
				.bind("metadataPath");
		metadataPath.setEnabled(false);
		metadataPublishing.addComponent(metadataPath);

		signMetadata = new CheckBox(msg.getMessage("SAMLAuthenticatorEditor.signMetadata"));
		configBinder.forField(signMetadata).bind("signMetadata");
		signMetadata.setEnabled(false);
		metadataPublishing.addComponent(signMetadata);

		CheckBox autoGenerateMetadata = new CheckBox(
				msg.getMessage("SAMLAuthenticatorEditor.autoGenerateMetadata"));
		configBinder.forField(autoGenerateMetadata).bind("autoGenerateMetadata");
		autoGenerateMetadata.setEnabled(false);
		metadataPublishing.addComponent(autoGenerateMetadata);

		FileField metadataSource = new FileField(msg, "text/xml", "metadata.xml",
				serverConfig.getFileSizeLimit());
		metadataSource.setCaption(msg.getMessage("SAMLAuthenticatorEditor.metadataFile"));
		metadataSource.configureBinding(configBinder, "metadataSource", Optional.of((value, context) -> {
			if (value != null && value.getLocal() != null)
			{
				try
				{
					EntityDescriptorDocument.Factory
							.parse(new ByteArrayInputStream(value.getLocal()));
				} catch (Exception e)
				{
					return ValidationResult.error(
							msg.getMessage("SAMLAuthenticatorEditor.invalidMetadataFile"));
				}
			}

			boolean isEmpty = value == null || (value.getLocal() == null
					&& (value.getRemote() == null || value.getRemote().isEmpty()));

			if (publishMetadata.getValue() && (!autoGenerateMetadata.getValue() && isEmpty))
			{
				return ValidationResult.error(msg.getMessage("SAMLAuthenticatorEditor.spMetaEmpty"));
			}

			return ValidationResult.ok();

		}));
		metadataSource.setEnabled(false);
		metadataPublishing.addComponent(metadataSource);

		publishMetadata.addValueChangeListener(e -> {
			boolean v = e.getValue();
			metadataPath.setEnabled(v);
			signMetadata.setEnabled(v);
			autoGenerateMetadata.setEnabled(v);
			metadataSource.setEnabled(!autoGenerateMetadata.getValue() && v);
		});

		autoGenerateMetadata.addValueChangeListener(e -> {
			metadataSource.setEnabled(!e.getValue() && publishMetadata.getValue());
		});

		return new CollapsibleLayout(msg.getMessage("SAMLAuthenticatorEditor.metadataPublishing"),
				metadataPublishing);
	}

	private CollapsibleLayout buildSingleLogoutSection()
	{
		FormLayoutWithFixedCaptionWidth singleLogout = new FormLayoutWithFixedCaptionWidth();
		singleLogout.setMargin(false);

		TextField sloPath = new TextField(msg.getMessage("SAMLAuthenticatorEditor.sloPath"));
		configBinder.forField(sloPath).bind("sloPath");
		singleLogout.addComponent(sloPath);

		ComboBox<String> sloRealm = new ComboBox<>(msg.getMessage("SAMLAuthenticatorEditor.sloRealm"));
		sloRealm.setItems(realms);
		singleLogout.addComponent(sloRealm);
		configBinder.forField(sloRealm).bind("sloRealm");

		GridWithEditor<SAMLIdentityMapping> sloMappings = new GridWithEditor<>(msg, SAMLIdentityMapping.class);
		sloMappings.setCaption(msg.getMessage("SAMLAuthenticatorEditor.sloMappings"));
		singleLogout.addComponent(sloMappings);
		sloMappings.addComboColumn(s -> s.getUnityId(), (t, v) -> t.setUnityId(v),
				msg.getMessage("SAMLAuthenticatorEditor.sloMappings.unityId"), idTypes, 30, false);
		sloMappings.addTextColumn(s -> s.getSamlId(), (t, v) -> t.setSamlId(v),
				msg.getMessage("SAMLAuthenticatorEditor.sloMappings.samlId"), 70, false);

		sloMappings.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(sloMappings).bind("sloMappings");

		return new CollapsibleLayout(msg.getMessage("SAMLAuthenticatorEditor.singleLogout"), singleLogout);
	}

	private boolean isSignReq()
	{
		boolean v = defSignRequest.getValue();

		if (idps != null && idps.getValue() != null)
		{
			for (SAMLIndividualTrustedSamlIdpConfiguration i : idps.getValue())
			{
				v |= i.isSignRequest();

			}
		}
		return v;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), SAMLVerificator.NAME, getConfiguration(), null);
	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();
		try
		{
			return configBinder.getBean().toProperties(pkiMan, fileStorageService, msg, getName());
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the SAML verificator", e);
		}
	}

	private Set<String> getRegistrationForms() throws EngineException
	{
		return registrationMan.getForms().stream().map(r -> r.getName()).collect(Collectors.toSet());
	}

	private class TrustedFederationComponent extends CustomField<List<SAMLAuthnTrustedFederationConfiguration>>
	{
		private GridWithActionColumn<SAMLAuthnTrustedFederationConfiguration> federationList;

		public TrustedFederationComponent()
		{
			federationList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
			federationList.addComponentColumn(
					p -> StandardButtonsHelper.buildLinkButton(p.getName(), e -> gotoEdit(p)),
					msg.getMessage("TrustedFederationComponent.name"), 50);
		}

		@Override
		protected Component initContent()
		{
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);
			main.addStyleName(Styles.narrowTable.toString());
			Button add = new Button();
			add.addClickListener(e -> gotoNew());
			add.setIcon(Images.add.getResource());
			main.addComponent(add);
			main.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);
			main.addComponent(federationList);
			return main;
		}

		private List<SingleActionHandler<SAMLAuthnTrustedFederationConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<SAMLAuthnTrustedFederationConfiguration> edit = SingleActionHandler
					.builder4Edit(msg, SAMLAuthnTrustedFederationConfiguration.class)
					.withHandler(r -> {
						SAMLAuthnTrustedFederationConfiguration edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<SAMLAuthnTrustedFederationConfiguration> remove = SingleActionHandler
					.builder4Delete(msg, SAMLAuthnTrustedFederationConfiguration.class)
					.withHandler(r -> {
						federationList.removeElement(r.iterator().next());
						fireChange();
					}).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoNew()
		{
			gotoEditSubView(null, federationList.getElements().stream().map(p -> p.getName())
					.collect(Collectors.toSet()), c -> {
						subViewSwitcher.exitSubViewAndShowUpdateInfo();
						federationList.addElement(c);
						federationList.focus();
						fireChange();
					});
		}

		private void gotoEdit(SAMLAuthnTrustedFederationConfiguration edited)
		{
			gotoEditSubView(edited,
					federationList.getElements().stream()
							.filter(p -> p.getName() != edited.getName())
							.map(p -> p.getName()).collect(Collectors.toSet()),
					c -> {
						federationList.replaceElement(edited, c);
						fireChange();
						subViewSwitcher.exitSubViewAndShowUpdateInfo();
					});
		}

		private void gotoEditSubView(SAMLAuthnTrustedFederationConfiguration edited, Set<String> usedNames,
				Consumer<SAMLAuthnTrustedFederationConfiguration> onConfirm)
		{
			Set<String> forms;
			Set<String> validators;
			Set<String> certificates;

			try
			{
				validators = pkiMan.getValidatorNames();
				certificates = pkiMan.getAllCertificateNames();
				forms = getRegistrationForms();

			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, "Can not init trusted federation editor", e);
				return;
			}

			EditTrustedFederationSubView subView = new EditTrustedFederationSubView(msg,
					profileFieldFactory, edited, subViewSwitcher, usedNames, validators,
					certificates, forms, r -> {
						onConfirm.accept(r);
						federationList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						federationList.focus();
					});
			subViewSwitcher.goToSubView(subView);

		}

		@Override
		public List<SAMLAuthnTrustedFederationConfiguration> getValue()
		{
			return federationList.getElements();
		}

		@Override
		protected void doSetValue(List<SAMLAuthnTrustedFederationConfiguration> value)
		{
			federationList.setItems(value);
		}

		private void fireChange()
		{
			fireEvent(new ValueChangeEvent<List<SAMLAuthnTrustedFederationConfiguration>>(this,
					federationList.getElements(), true));
		}
	}

	private class IndividualTrustedIdpComponent extends CustomField<List<SAMLIndividualTrustedSamlIdpConfiguration>>
	{
		private GridWithActionColumn<SAMLIndividualTrustedSamlIdpConfiguration> idpList;

		public IndividualTrustedIdpComponent()
		{
			idpList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
			idpList.addComponentColumn(
					p -> StandardButtonsHelper.buildLinkButton(p.getName(), e -> gotoEdit(p)),
					msg.getMessage("IndividualTrustedIdpComponent.name"), 50);
		}

		@Override
		public List<SAMLIndividualTrustedSamlIdpConfiguration> getValue()
		{
			return idpList.getElements();
		}

		@Override
		protected Component initContent()
		{
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);
			main.addStyleName(Styles.narrowTable.toString());
			Button add = new Button();
			add.addClickListener(e -> gotoNew());
			add.setIcon(Images.add.getResource());
			main.addComponent(add);
			main.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);
			main.addComponent(idpList);
			return main;
		}

		private List<SingleActionHandler<SAMLIndividualTrustedSamlIdpConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<SAMLIndividualTrustedSamlIdpConfiguration> edit = SingleActionHandler
					.builder4Edit(msg, SAMLIndividualTrustedSamlIdpConfiguration.class)
					.withHandler(r -> {
						SAMLIndividualTrustedSamlIdpConfiguration edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<SAMLIndividualTrustedSamlIdpConfiguration> remove = SingleActionHandler
					.builder4Delete(msg, SAMLIndividualTrustedSamlIdpConfiguration.class)
					.withHandler(r -> {
						idpList.removeElement(r.iterator().next());
						fireChange();
					}).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoNew()
		{
			gotoEditSubView(null, idpList.getElements().stream().map(p -> p.getName())
					.collect(Collectors.toSet()), c -> {
						subViewSwitcher.exitSubViewAndShowUpdateInfo();
						idpList.addElement(c);
						idpList.focus();
						fireChange();
					});
		}

		private void gotoEdit(SAMLIndividualTrustedSamlIdpConfiguration edited)
		{
			gotoEditSubView(edited,
					idpList.getElements().stream().filter(p -> p.getName() != edited.getName())
							.map(p -> p.getName()).collect(Collectors.toSet()),
					c -> {
						idpList.replaceElement(edited, c);
						fireChange();
						subViewSwitcher.exitSubViewAndShowUpdateInfo();
					});
		}

		private void gotoEditSubView(SAMLIndividualTrustedSamlIdpConfiguration edited, Set<String> usedNames,
				Consumer<SAMLIndividualTrustedSamlIdpConfiguration> onConfirm)
		{
			Set<String> forms;
			Set<String> certificates;

			try
			{
				certificates = pkiMan.getAllCertificateNames();
				forms = getRegistrationForms();

			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, "Can not init trusted IdP editor", e);
				return;
			}

			EditIndividualTrustedIdpSubView subView = new EditIndividualTrustedIdpSubView(msg, serverConfig,
					uriAccessService, profileFieldFactory, edited, subViewSwitcher, usedNames,
					certificates, forms, r -> {
						onConfirm.accept(r);
						idpList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						idpList.focus();
					});
			subViewSwitcher.goToSubView(subView);

		}

		@Override
		protected void doSetValue(List<SAMLIndividualTrustedSamlIdpConfiguration> value)
		{
			idpList.setItems(value);
		}

		private void fireChange()
		{
			fireEvent(new ValueChangeEvent<List<SAMLIndividualTrustedSamlIdpConfiguration>>(this,
					idpList.getElements(), true));
		}

	}
}
