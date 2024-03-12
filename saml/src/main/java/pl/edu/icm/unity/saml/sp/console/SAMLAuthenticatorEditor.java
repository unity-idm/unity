/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.console;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.TooltipFactory;
import io.imunity.vaadin.elements.grid.EditableGrid;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.file.FileField;
import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.saml.console.SAMLIdentityMapping;
import pl.edu.icm.unity.saml.sp.SAMLVerificator;
import pl.edu.icm.unity.webui.common.FormValidationException;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorDocument;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


class SAMLAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	public static final List<String> STANDART_NAME_FORMATS = Arrays.asList(
			"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
			"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
			"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName",
			"urn:oasis:names:tc:SAML:2.0:nameid-format:transient");

	private final PKIManagement pkiMan;
	private final FileStorageService fileStorageService;
	private final URIAccessService uriAccessService;
	private final UnityServerConfiguration serverConfig;
	private final InputTranslationProfileFieldFactory profileFieldFactory;
	private final RegistrationsManagement registrationMan;
	private Binder<SAMLAuthenticatorConfiguration> configBinder;
	private SubViewSwitcher subViewSwitcher;

	private final Set<String> credentials;
	private final Set<String> realms;
	private final List<String> idTypes;

	private Checkbox defSignRequest;
	private IndividualTrustedIdpComponent idps;
	private Checkbox signMetadata;
	private final VaadinLogoImageLoader imageAccessService;
	private final NotificationPresenter notificationPresenter;
	private final SharedEndpointManagement sharedEndpointManagement;

	SAMLAuthenticatorEditor(MessageSource msg, UnityServerConfiguration serverConfig, PKIManagement pkiMan,
			InputTranslationProfileFieldFactory profileFieldFactory,
			RegistrationsManagement registrationMan, RealmsManagement realmMan,
			IdentityTypesRegistry idTypesReg, FileStorageService fileStorageService,
			URIAccessService uriAccessService, VaadinLogoImageLoader imageAccessService,
			NotificationPresenter notificationPresenter, SharedEndpointManagement sharedEndpointManagement) throws EngineException
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
		this.realms = realmMan.getRealms().stream().map(DescribedObjectROImpl::getName).collect(Collectors.toSet());
		this.idTypes = idTypesReg.getAll().stream().map(IdentityTypeDefinition::getId).collect(Collectors.toList());
		this.notificationPresenter = notificationPresenter;
		this.sharedEndpointManagement = sharedEndpointManagement;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		this.subViewSwitcher = subViewSwitcher;

		boolean editMode = init(msg.getMessage("SAMLAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(SAMLAuthenticatorConfiguration.class);

		FormLayout header = buildHeaderSection();

		AccordionPanel trustedFederations = buildTrustedFederationsSection();
		trustedFederations.setOpened(true);
		trustedFederations.setWidthFull();
		AccordionPanel individualTrustedIdPs = buildIndividualTrustedIdPsSection();
		individualTrustedIdPs.setWidthFull();
		AccordionPanel metadataPublishing = buildSAMLMetadaPublishingSection();
		metadataPublishing.setWidthFull();
		AccordionPanel singleLogout = buildSingleLogoutSection();
		singleLogout.setWidthFull();

		VerticalLayout mainView = new VerticalLayout();
		mainView.setPadding(false);
		mainView.add(header);
		mainView.add(trustedFederations, individualTrustedIdPs, metadataPublishing, singleLogout);

		SAMLAuthenticatorConfiguration config = new SAMLAuthenticatorConfiguration();
		if (editMode)
		{
			config.fromProperties(pkiMan, uriAccessService, imageAccessService, msg, toEdit.configuration);
		}

		configBinder.setBean(config);

		return mainView;
	}

	private FormLayout buildHeaderSection()
	{
		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));
		name.focus();

		TextField requesterId = new TextField();
		requesterId.setWidth(TEXT_FIELD_MEDIUM.value());
		configBinder.forField(requesterId)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind(SAMLAuthenticatorConfiguration::getRequesterId, SAMLAuthenticatorConfiguration::setRequesterId);
		header.addFormItem(requesterId, msg.getMessage("SAMLAuthenticatorEditor.requesterId"));

		ComboBox<String> credential = new ComboBox<>();
		credential.setItems(credentials);
		configBinder.forField(credential).asRequired(
				(v, c) -> ((v == null || v.isEmpty()) && (isSignReq() || signMetadata.getValue()))
						? ValidationResult.error(msg.getMessage("fieldRequired"))
						: ValidationResult.ok())
				.bind(SAMLAuthenticatorConfiguration::getCredential, SAMLAuthenticatorConfiguration::setCredential);
		header.addFormItem(credential, msg.getMessage("SAMLAuthenticatorEditor.credential"));
		
		ComboBox<String> additionalCredential = new ComboBox<>();
		additionalCredential.setItems(credentials);
		configBinder.forField(additionalCredential)
				.bind(SAMLAuthenticatorConfiguration::getAdditionalCredential, SAMLAuthenticatorConfiguration::setAdditionalCredential);
		header.addFormItem(additionalCredential, msg.getMessage("SAMLAuthenticatorEditor.additionalCredential"))
				.add(TooltipFactory.get(msg.getMessage("SAMLAuthenticatorEditor.additionalCredentialDesc")));

		Checkbox includeAddtionalCredentialInMetadata = new Checkbox(msg.getMessage("SAMLAuthenticatorEditor.includeAddtionalCredentialInMetadata"));
		configBinder.forField(includeAddtionalCredentialInMetadata)
				.bind(SAMLAuthenticatorConfiguration::isIncludeAdditionalCredentialInMetadata, SAMLAuthenticatorConfiguration::setIncludeAdditionalCredentialInMetadata);
		header.addFormItem(includeAddtionalCredentialInMetadata, "");
			
		MultiSelectComboBox<String> acceptedNameFormats = new MultiSelectComboBox<>();
		acceptedNameFormats.setItems(STANDART_NAME_FORMATS);
		acceptedNameFormats.setWidth(TEXT_FIELD_BIG.value());
		header.addFormItem(acceptedNameFormats, msg.getMessage("SAMLAuthenticatorEditor.acceptedNameFormats"));
		configBinder.forField(acceptedNameFormats)
				.withConverter(List::copyOf, HashSet::new)
				.bind(SAMLAuthenticatorConfiguration::getAcceptedNameFormats, SAMLAuthenticatorConfiguration::setAcceptedNameFormats);

		Checkbox requireSignedAssertion = new Checkbox(
				msg.getMessage("SAMLAuthenticatorEditor.requireSignedAssertion"));
		configBinder.forField(requireSignedAssertion)
				.bind(SAMLAuthenticatorConfiguration::isRequireSignedAssertion, SAMLAuthenticatorConfiguration::setRequireSignedAssertion);
		header.addFormItem(requireSignedAssertion, "");

		defSignRequest = new Checkbox(msg.getMessage("SAMLAuthenticatorEditor.defSignRequest"));
		configBinder.forField(defSignRequest)
				.bind(SAMLAuthenticatorConfiguration::isDefSignRequest, SAMLAuthenticatorConfiguration::setDefSignRequest);
		header.addFormItem(defSignRequest, "");

		ComboBox<String> defaultRequestedNameFormat = new ComboBox<>();
		Set<String> items = new HashSet<>(STANDART_NAME_FORMATS);
		defaultRequestedNameFormat.setItems(items);
		defaultRequestedNameFormat.setPlaceholder(msg.getMessage("typeOrSelect"));
		defaultRequestedNameFormat.setWidth(TEXT_FIELD_BIG.value());
		defaultRequestedNameFormat.setAllowCustomValue(true);
		defaultRequestedNameFormat.addCustomValueSetListener(event ->
		{
			items.add(event.getDetail());
			defaultRequestedNameFormat.setItems(items);
			defaultRequestedNameFormat.setValue(event.getDetail());
		});
		header.addFormItem(defaultRequestedNameFormat, msg.getMessage("SAMLAuthenticatorEditor.defaultRequestedNameFormat"));
		configBinder.forField(defaultRequestedNameFormat)
				.withConverter(item ->
				{
					if(item == null)
						return List.<String>of();
					return List.of(item);
				},
					names -> names.stream().findFirst().orElse(null))
				.bind(SAMLAuthenticatorConfiguration::getDefaultRequestedNameFormat, SAMLAuthenticatorConfiguration::setDefaultRequestedNameFormat);

		Checkbox defAccountAssociation = new Checkbox(
				msg.getMessage("SAMLAuthenticatorEditor.defAccountAssociation"));
		configBinder.forField(defAccountAssociation)
				.bind(SAMLAuthenticatorConfiguration::isDefAccountAssociation, SAMLAuthenticatorConfiguration::setDefAccountAssociation);
		header.addFormItem(defAccountAssociation, "");

		return header;
	}

	private AccordionPanel buildTrustedFederationsSection()
	{
		FormLayout trustedFederations = new FormLayout();
		trustedFederations.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		trustedFederations.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		TrustedFederationComponent federations = new TrustedFederationComponent();
		configBinder.forField(federations)
				.bind(SAMLAuthenticatorConfiguration::getTrustedFederations, SAMLAuthenticatorConfiguration::setTrustedFederations);
		trustedFederations.addFormItem(federations, "");

		return new AccordionPanel(msg.getMessage("SAMLAuthenticatorEditor.trustedFederations"),
				trustedFederations);
	}

	private AccordionPanel buildIndividualTrustedIdPsSection()
	{
		FormLayout individualTrustedIdPs = new FormLayout();
		individualTrustedIdPs.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		individualTrustedIdPs.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		idps = new IndividualTrustedIdpComponent();
		configBinder.forField(idps)
				.bind(SAMLAuthenticatorConfiguration::getIndividualTrustedIdps, SAMLAuthenticatorConfiguration::setIndividualTrustedIdps);
		individualTrustedIdPs.addFormItem(idps, "");

		return new AccordionPanel(msg.getMessage("SAMLAuthenticatorEditor.individualTrustedIdPs"),
				individualTrustedIdPs);
	}

	private AccordionPanel buildSAMLMetadaPublishingSection()
	{
		FormLayout metadataPublishing = new FormLayout();
		metadataPublishing.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		metadataPublishing.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		Checkbox publishMetadata = new Checkbox(msg.getMessage("SAMLAuthenticatorEditor.publishMetadata"));
		configBinder.forField(publishMetadata)
				.bind(SAMLAuthenticatorConfiguration::isPublishMetadata, SAMLAuthenticatorConfiguration::setPublishMetadata);
		metadataPublishing.addFormItem(publishMetadata, "");

		TextField metadataPath = new TextField();
		configBinder.forField(metadataPath)
				.asRequired((v, c) -> ((v == null || v.isEmpty()) && publishMetadata.getValue())
						? ValidationResult.error(msg.getMessage("fieldRequired"))
						: ValidationResult.ok())
				.bind(SAMLAuthenticatorConfiguration::getMetadataPath, SAMLAuthenticatorConfiguration::setMetadataPath);
		metadataPath.setEnabled(false);
		metadataPublishing.addFormItem(metadataPath, msg.getMessage("SAMLAuthenticatorEditor.metadataPath"));

		TextField urlMetadataPathField = new TextField();
		urlMetadataPathField.setWidth(TEXT_FIELD_BIG.value());
		urlMetadataPathField.setReadOnly(true);
		metadataPublishing.addFormItem(urlMetadataPathField, msg.getMessage("SAMLAuthenticatorEditor.metadataUrl"));
		metadataPath.addValueChangeListener(item -> urlMetadataPathField.setValue(
				sharedEndpointManagement.getServletUrl("/saml-sp-metadata/") + item.getValue()));
		configBinder.forField(urlMetadataPathField)
				.bindReadOnly(item -> sharedEndpointManagement.getServletUrl("/saml-sp-metadata/") + item.getMetadataPath());

		signMetadata = new Checkbox(msg.getMessage("SAMLAuthenticatorEditor.signMetadata"));
		configBinder.forField(signMetadata)
				.bind(SAMLAuthenticatorConfiguration::isSignMetadata, SAMLAuthenticatorConfiguration::setSignMetadata);
		signMetadata.setEnabled(false);
		metadataPublishing.addFormItem(signMetadata, "");

		Checkbox autoGenerateMetadata = new Checkbox(
				msg.getMessage("SAMLAuthenticatorEditor.autoGenerateMetadata"));
		configBinder.forField(autoGenerateMetadata)
				.bind(SAMLAuthenticatorConfiguration::isAutoGenerateMetadata, SAMLAuthenticatorConfiguration::setAutoGenerateMetadata);
		autoGenerateMetadata.setEnabled(false);
		metadataPublishing.addFormItem(autoGenerateMetadata, "");
		autoGenerateMetadata.addValueChangeListener(event ->
		{
			if(event.getValue())
				urlMetadataPathField.setValue(
						sharedEndpointManagement.getServletUrl("/saml-sp-metadata/") + metadataPath.getValue());
			else
				urlMetadataPathField.setValue("");
		});

		FileField metadataSource = new FileField(msg, "text/xml", "metadata.xml",
				serverConfig.getFileSizeLimit());
		metadataSource.setSizeFull();
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
					&& (value.getSrc() == null || value.getSrc().isEmpty()));

			if (publishMetadata.getValue() && (!autoGenerateMetadata.getValue() && isEmpty))
			{
				return ValidationResult.error(msg.getMessage("SAMLAuthenticatorEditor.spMetaEmpty"));
			}

			return ValidationResult.ok();

		}));
		metadataSource.setEnabled(false);
		metadataPublishing.addFormItem(metadataSource, msg.getMessage("SAMLAuthenticatorEditor.metadataFile"));

		publishMetadata.addValueChangeListener(e -> {
			boolean v = e.getValue();
			metadataPath.setEnabled(v);
			signMetadata.setEnabled(v);
			if(v)
				urlMetadataPathField.setValue(sharedEndpointManagement.getServletUrl("/saml-sp-metadata/") + metadataPath.getValue());
			else
				urlMetadataPathField.setValue("");
			autoGenerateMetadata.setEnabled(v);
			metadataSource.setEnabled(!autoGenerateMetadata.getValue() && v);
		});

		autoGenerateMetadata.addValueChangeListener(e ->
		{
			metadataSource.setEnabled(!e.getValue() && publishMetadata.getValue());
			if(e.getValue())
				metadataSource.setValue(new LocalOrRemoteResource());
		});

		return new AccordionPanel(msg.getMessage("SAMLAuthenticatorEditor.metadataPublishing"),
				metadataPublishing);
	}

	private AccordionPanel buildSingleLogoutSection()
	{
		FormLayout singleLogout = new FormLayout();
		singleLogout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		singleLogout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		TextField sloPath = new TextField();
		configBinder.forField(sloPath).bind(SAMLAuthenticatorConfiguration::getSloPath, SAMLAuthenticatorConfiguration::setSloPath);
		singleLogout.addFormItem(sloPath, msg.getMessage("SAMLAuthenticatorEditor.sloPath"));

		Select<String> sloRealm = new Select<>();
		sloRealm.setItems(realms);
		sloRealm.setEmptySelectionAllowed(true);
		singleLogout.addFormItem(sloRealm, msg.getMessage("SAMLAuthenticatorEditor.sloRealm"));
		configBinder.forField(sloRealm).bind(SAMLAuthenticatorConfiguration::getSloRealm, SAMLAuthenticatorConfiguration::setSloRealm);

		EditableGrid<SAMLIdentityMapping> sloMappings = new EditableGrid<>(msg::getMessage,
				() -> new SAMLIdentityMapping(null, idTypes.iterator().next()));
		sloMappings.setWidth("40em");
		sloMappings.setHeight("20em");
		singleLogout.addFormItem(sloMappings, msg.getMessage("SAMLAuthenticatorEditor.sloMappings"));
		sloMappings.addComboBoxColumn(SAMLIdentityMapping::getUnityId, SAMLIdentityMapping::setUnityId, idTypes)
				.setHeader(msg.getMessage("SAMLAuthenticatorEditor.sloMappings.unityId"))
				.setAutoWidth(true);
		sloMappings.addColumn(SAMLIdentityMapping::getSamlId, SAMLIdentityMapping::setSamlId, false)
				.setHeader(msg.getMessage("SAMLAuthenticatorEditor.sloMappings.samlId"))
				.setAutoWidth(true);
		sloMappings.enableEditorOnSelect();
		configBinder.forField(sloMappings).bind(SAMLAuthenticatorConfiguration::getSloMappings, SAMLAuthenticatorConfiguration::setSloMappings);

		return new AccordionPanel(msg.getMessage("SAMLAuthenticatorEditor.singleLogout"), singleLogout);
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
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
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
		return registrationMan.getForms().stream()
				.map(DescribedObjectROImpl::getName)
				.collect(Collectors.toSet());
	}

	private class TrustedFederationComponent extends CustomField<List<SAMLAuthnTrustedFederationConfiguration>>
	{
		private GridWithActionColumn<SAMLAuthnTrustedFederationConfiguration> federationList;

		public TrustedFederationComponent()
		{
			federationList = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
			federationList.addComponentColumn(p -> new LinkButton(p.getName(), e -> gotoEdit(p)))
							.setHeader(msg.getMessage("TrustedFederationComponent.name"));
			federationList.setHeight("15em");
			add(initContent());
			setWidth("25em");
			setHeight("20em");
		}

		@Override
		protected List<SAMLAuthnTrustedFederationConfiguration> generateModelValue()
		{
			return getValue();
		}

		@Override
		protected void setPresentationValue(
				List<SAMLAuthnTrustedFederationConfiguration> samlAuthnTrustedFederationConfigurations)
		{
			federationList.setItems(samlAuthnTrustedFederationConfigurations);
		}

		protected Component initContent()
		{
			VerticalLayout main = new VerticalLayout();
			main.setPadding(false);
			Button add = new Button();
			add.addClickListener(e -> gotoNew());
			add.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
			main.add(add, federationList);
			main.setAlignItems(FlexComponent.Alignment.END);
			return main;
		}

		private List<SingleActionHandler<SAMLAuthnTrustedFederationConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<SAMLAuthnTrustedFederationConfiguration> edit = SingleActionHandler
					.builder4Edit(msg::getMessage, SAMLAuthnTrustedFederationConfiguration.class)
					.withHandler(r -> {
						SAMLAuthnTrustedFederationConfiguration edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<SAMLAuthnTrustedFederationConfiguration> remove = SingleActionHandler
					.builder4Delete(msg::getMessage, SAMLAuthnTrustedFederationConfiguration.class)
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
							.map(SAMLAuthnTrustedFederationConfiguration::getName)
							.filter(pName -> !Objects.equals(pName, edited.getName()))
							.collect(Collectors.toSet()),
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
				notificationPresenter.showError("Can not init trusted federation editor", e.getMessage());
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
					}, notificationPresenter);
			subViewSwitcher.goToSubView(subView);

		}

		@Override
		public List<SAMLAuthnTrustedFederationConfiguration> getValue()
		{
			return federationList.getElements();
		}


		private void fireChange()
		{
			fireEvent(new ComponentValueChangeEvent<>(this, this,
					federationList.getElements(), false));
		}
	}

	private class IndividualTrustedIdpComponent extends CustomField<List<SAMLIndividualTrustedSamlIdpConfiguration>>
	{
		private GridWithActionColumn<SAMLIndividualTrustedSamlIdpConfiguration> idpList;

		public IndividualTrustedIdpComponent()
		{
			idpList = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
			idpList.setHeight("15em");
			idpList.addComponentColumn(p -> new LinkButton(p.getName(), e -> gotoEdit(p)))
							.setHeader(msg.getMessage("IndividualTrustedIdpComponent.name"));
			add(initContent());
			setWidth("25em");
			setHeight("20em");
		}

		@Override
		protected List<SAMLIndividualTrustedSamlIdpConfiguration> generateModelValue()
		{
			return getValue();
		}

		@Override
		protected void setPresentationValue(
				List<SAMLIndividualTrustedSamlIdpConfiguration> samlIndividualTrustedSamlIdpConfigurations)
		{
			idpList.setItems(samlIndividualTrustedSamlIdpConfigurations);
		}

		@Override
		public List<SAMLIndividualTrustedSamlIdpConfiguration> getValue()
		{
			return idpList.getElements();
		}

		protected Component initContent()
		{
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);
			Button add = new Button();
			add.addClickListener(e -> gotoNew());
			add.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
			main.add(add, idpList);
			main.setAlignItems(FlexComponent.Alignment.END);
			return main;
		}

		private List<SingleActionHandler<SAMLIndividualTrustedSamlIdpConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<SAMLIndividualTrustedSamlIdpConfiguration> edit = SingleActionHandler
					.builder4Edit(msg::getMessage, SAMLIndividualTrustedSamlIdpConfiguration.class)
					.withHandler(r -> {
						SAMLIndividualTrustedSamlIdpConfiguration edited = r.iterator().next();
						gotoEdit(edited);
					}

					).build();

			SingleActionHandler<SAMLIndividualTrustedSamlIdpConfiguration> remove = SingleActionHandler
					.builder4Delete(msg::getMessage, SAMLIndividualTrustedSamlIdpConfiguration.class)
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
					idpList.getElements().stream().map(SAMLIndividualTrustedSamlIdpConfiguration::getName)
							.filter(pName -> !Objects.equals(pName, edited.getName())).collect(Collectors.toSet()),
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
				notificationPresenter.showError( "Can not init trusted IdP editor", e.getMessage());
				return;
			}

			EditIndividualTrustedIdpSubView subView = new EditIndividualTrustedIdpSubView(msg, notificationPresenter,
					profileFieldFactory, edited, subViewSwitcher, usedNames,
					certificates, forms, r -> {
						onConfirm.accept(r);
						idpList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						idpList.focus();
					}, serverConfig);
			subViewSwitcher.goToSubView(subView);

		}

		private void fireChange()
		{
			fireEvent(new ComponentValueChangeEvent<>(this, this, idpList.getElements(), false));
		}

	}
}
