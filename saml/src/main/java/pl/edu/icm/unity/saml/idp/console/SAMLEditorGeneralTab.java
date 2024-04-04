/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import io.imunity.console.utils.tprofile.OutputTranslationProfileFieldFactory;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.TooltipFactory;
import io.imunity.vaadin.elements.grid.EditableGrid;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import io.imunity.vaadin.endpoint.common.file.FileField;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointPathValidator;
import pl.edu.icm.unity.saml.console.SAMLIdentityMapping;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration.AssertionSigningPolicy;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration.RequestAcceptancePolicy;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration.ResponseSigningPolicy;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorDocument;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.*;

/**
 * SAML service editor general tab
 * 
 * @author P.Piernik
 *
 */
public class SAMLEditorGeneralTab extends VerticalLayout implements ServiceEditorBase.EditorTab
{
	private final MessageSource msg;
	private Binder<DefaultServiceDefinition> samlServiceBinder;
	private Binder<SAMLServiceConfiguration> configBinder;
	private final OutputTranslationProfileFieldFactory profileFieldFactory;
	private final UnityServerConfiguration serverConfig;
	private final SubViewSwitcher subViewSwitcher;
	private final Set<String> credentials;
	private final Set<String> truststores;
	private final List<String> usedEndpointsPaths;
	private final Set<String> serverContextPaths;
	private final String serverPrefix;
	private final Collection<IdentityType> idTypes;
	private boolean editMode;
	private Checkbox signMetadata;
	private boolean initialValidation;
	private Button metaLinkButton;
	private HorizontalLayout metaLinkButtonWrapper;
	private Span metaOffInfo;
	
	public SAMLEditorGeneralTab(MessageSource msg, String serverPrefix, Set<String> serverContextPaths, UnityServerConfiguration serverConfig,
			SubViewSwitcher subViewSwitcher, OutputTranslationProfileFieldFactory profileFieldFactory,
			List<String> usedEndpointsPaths,
			Set<String> credentials, Set<String> truststores, Collection<IdentityType> idTypes)
	{
		this.msg = msg;
		this.serverConfig = serverConfig;
		this.subViewSwitcher = subViewSwitcher;
		this.profileFieldFactory = profileFieldFactory;
		this.usedEndpointsPaths = usedEndpointsPaths;
		this.credentials = credentials;
		this.truststores = truststores;
		this.idTypes = idTypes;
		this.serverPrefix = serverPrefix;
		this.serverContextPaths = serverContextPaths;
		
	}

	public void initUI(Binder<DefaultServiceDefinition> samlServiceBinder,
			Binder<SAMLServiceConfiguration> configBinder, boolean editMode)
	{
		this.samlServiceBinder = samlServiceBinder;
		this.configBinder = configBinder;
		this.editMode = editMode;

		setPadding(false);
		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		main.add(buildHeaderSection());
		main.add(buildMetadataSection());
		main.add(buildAdvancedSection());
		main.add(buildIdenityTypeMappingSection());
		main.add(profileFieldFactory.getWrappedFieldInstance(subViewSwitcher, configBinder,
				"translationProfile"));
		add(main);
	}

	private Component buildHeaderSection()
	{
		HorizontalLayout main = new HorizontalLayout();

		FormLayout mainGeneralLayout = new FormLayout();
		mainGeneralLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		mainGeneralLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		main.add(mainGeneralLayout);

		metaLinkButton = new Button();
		metaOffInfo = new Span();

		HorizontalLayout infoLayout = new HorizontalLayout();
		infoLayout.addClassName(IDP_INFO_LAYOUT.getName());

		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setPadding(false);
		infoLayout.add(wrapper);
		wrapper.add(new Span(msg.getMessage("SAMLEditorGeneralTab.importantURLs")));

		FormLayout infoLayoutWrapper = new FormLayout();
		infoLayoutWrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		wrapper.add(infoLayoutWrapper);
		metaLinkButtonWrapper = new HorizontalLayout();
		metaLinkButtonWrapper.add(metaLinkButton);
		infoLayoutWrapper.addFormItem(metaLinkButtonWrapper, msg.getMessage("SAMLEditorGeneralTab.metadataLink"));
		infoLayoutWrapper.addFormItem(metaOffInfo, msg.getMessage("SAMLEditorGeneralTab.metadataOff")).setVisible(false);

		metaLinkButton.addClickListener(e -> UI.getCurrent().getPage().open(metaLinkButton.getText(), "_blank"));
		metaLinkButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		main.add(infoLayout);
		refreshMetaButton(false);
				
		TextField name = new TextField();
		name.setReadOnly(editMode);
		samlServiceBinder.forField(name).asRequired()
				.bind(DefaultServiceDefinition::getName, DefaultServiceDefinition::setName);
		mainGeneralLayout.addFormItem(name, msg.getMessage("ServiceEditorBase.name"));

		TextField contextPath = new TextField();
		contextPath.setPlaceholder("/saml-idp");
		contextPath.setRequiredIndicatorVisible(true);
		contextPath.setReadOnly(editMode);
		samlServiceBinder.forField(contextPath).withValidator((v, c) -> {			
			ValidationResult r;
			if (editMode)
			{
				r = validatePathForEdit(v);
			}else
			{
				r = validatePathForAdd(v);
			}
			
			if (!r.isError())
			{
				metaLinkButton.setText(serverPrefix + v + "/metadata");
			}
			
			return r;
		}).bind(DefaultServiceDefinition::getAddress, DefaultServiceDefinition::setAddress);
		mainGeneralLayout.addFormItem(contextPath, msg.getMessage("SAMLEditorGeneralTab.contextPath"));

		LocalizedTextFieldDetails displayedName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		samlServiceBinder.forField(displayedName)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)				.bind(DefaultServiceDefinition::getDisplayedName, DefaultServiceDefinition::setDisplayedName);
		mainGeneralLayout.addFormItem(displayedName, msg.getMessage("ServiceEditorBase.displayedName"));

		TextField description = new TextField();
		description.setWidth(TEXT_FIELD_BIG.value());
		samlServiceBinder.forField(description)
				.bind(DefaultServiceDefinition::getDescription, DefaultServiceDefinition::setDescription);
		mainGeneralLayout.addFormItem(description, msg.getMessage("ServiceEditorBase.description"));

		TextField issuerURI = new TextField();
		issuerURI.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(issuerURI).asRequired()
				.bind(SAMLServiceConfiguration::getIssuerURI, SAMLServiceConfiguration::setIssuerURI);
		mainGeneralLayout.addFormItem(issuerURI, msg.getMessage("SAMLEditorGeneralTab.issuerURI"));

		Select<AssertionSigningPolicy> signAssertionPolicy = new Select<>();
		signAssertionPolicy.setItems(AssertionSigningPolicy.values());
		configBinder.forField(signAssertionPolicy).asRequired()
				.bind(SAMLServiceConfiguration::getSignAssertionPolicy, SAMLServiceConfiguration::setSignAssertionPolicy);
		mainGeneralLayout.addFormItem(signAssertionPolicy, msg.getMessage("SAMLEditorGeneralTab.signAssertionPolicy"));

		Select<ResponseSigningPolicy> signResponcePolicy = new Select<>();
		signResponcePolicy.setItems(ResponseSigningPolicy.values());
		configBinder.forField(signResponcePolicy).asRequired()
				.bind(SAMLServiceConfiguration::getSignResponcePolicy, SAMLServiceConfiguration::setSignResponcePolicy);
		mainGeneralLayout.addFormItem(signResponcePolicy, msg.getMessage("SAMLEditorGeneralTab.signResponcePolicy"));

		ComboBox<String> signResponseCredential = new ComboBox<>();
		signResponseCredential.setItems(credentials);
		configBinder.forField(signResponseCredential)
				.asRequired((v, c) -> ((v == null || v.isEmpty()))
						? ValidationResult.error(msg.getMessage("fieldRequired"))
						: ValidationResult.ok())
				.bind(SAMLServiceConfiguration::getSignResponseCredential, SAMLServiceConfiguration::setSignResponseCredential);
		mainGeneralLayout.addFormItem(signResponseCredential, msg.getMessage("SAMLEditorGeneralTab.signResponseCredential"));

		ComboBox<String> additionallyAdvertisedCredential = new ComboBox<>();
		additionallyAdvertisedCredential.setItems(credentials);
		configBinder.forField(additionallyAdvertisedCredential)
				.bind(SAMLServiceConfiguration::getAdditionallyAdvertisedCredential, SAMLServiceConfiguration::setAdditionallyAdvertisedCredential);
		mainGeneralLayout.addFormItem(additionallyAdvertisedCredential, msg.getMessage("SAMLEditorGeneralTab.additionallyAdvertisedCredential"))
				.add(TooltipFactory.get(msg.getMessage("SAMLEditorGeneralTab.additionallyAdvertisedCredentialDesc")));
		
		ComboBox<String> httpsTruststore = new ComboBox<>();
		httpsTruststore.setItems(truststores);
		configBinder.forField(httpsTruststore)
				.bind(SAMLServiceConfiguration::getHttpsTruststore, SAMLServiceConfiguration::setHttpsTruststore);
		mainGeneralLayout.addFormItem(httpsTruststore, msg.getMessage("SAMLEditorGeneralTab.httpsTruststore"));

		Checkbox skipConsentScreen = new Checkbox(msg.getMessage("SAMLEditorGeneralTab.skipConsentScreen"));
		configBinder.forField(skipConsentScreen)
				.bind(SAMLServiceConfiguration::isSkipConsentScreen, SAMLServiceConfiguration::setSkipConsentScreen);
		mainGeneralLayout.addFormItem(skipConsentScreen, "");

		Checkbox editableConsentScreen = new Checkbox(
				msg.getMessage("SAMLEditorGeneralTab.editableConsentScreen"));
		configBinder.forField(editableConsentScreen)
				.bind(SAMLServiceConfiguration::isEditableConsentScreen, SAMLServiceConfiguration::setEditableConsentScreen);
		mainGeneralLayout.addFormItem(editableConsentScreen, "");

		skipConsentScreen.addValueChangeListener(e -> editableConsentScreen.setEnabled(!e.getValue()));

		Select<RequestAcceptancePolicy> acceptPolicy = new Select<>();
		acceptPolicy.setItems(RequestAcceptancePolicy.values());
		configBinder.forField(acceptPolicy).asRequired()
				.bind(SAMLServiceConfiguration::getRequestAcceptancePolicy, SAMLServiceConfiguration::setRequestAcceptancePolicy);
		mainGeneralLayout.addFormItem(acceptPolicy, msg.getMessage("SAMLEditorGeneralTab.acceptPolicy"));

		Checkbox sendNotBefore = new Checkbox(
				msg.getMessage("SAMLEditorGeneralTab.sendNotBefore"));
		configBinder.forField(sendNotBefore)
				.bind(SAMLServiceConfiguration::isSendNotBeforeConstraint, SAMLServiceConfiguration::setSendNotBeforeConstraint);
		mainGeneralLayout.addFormItem(sendNotBefore, "");

		
		return main;
	}

	private AccordionPanel buildAdvancedSection()
	{
		FormLayout advancedLayout = new FormLayout();
		advancedLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		advancedLayout.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());

		IntegerField authenticationTimeout = new IntegerField();
		authenticationTimeout.setStepButtonsVisible(true);
		configBinder.forField(authenticationTimeout).asRequired(msg.getMessage("notAPositiveNumber"))
				.bind(SAMLServiceConfiguration::getAuthenticationTimeout, SAMLServiceConfiguration::setAuthenticationTimeout);
		advancedLayout.addFormItem(authenticationTimeout, msg.getMessage("SAMLEditorGeneralTab.authenticationTimeout"));

		IntegerField requestValidity = new IntegerField();
		requestValidity.setStepButtonsVisible(true);
		configBinder.forField(requestValidity).asRequired(msg.getMessage("notAPositiveNumber"))
				.bind(SAMLServiceConfiguration::getRequestValidity, SAMLServiceConfiguration::setRequestValidity);
		advancedLayout.addFormItem(requestValidity, msg.getMessage("SAMLEditorGeneralTab.requestValidity"));

		IntegerField attrAssertionValidity = new IntegerField();
		attrAssertionValidity.setStepButtonsVisible(true);
		configBinder.forField(attrAssertionValidity).asRequired(msg.getMessage("notAPositiveNumber"))
				.bind(SAMLServiceConfiguration::getAttrAssertionValidity, SAMLServiceConfiguration::setAttrAssertionValidity);
		advancedLayout.addFormItem(attrAssertionValidity, msg.getMessage("SAMLEditorGeneralTab.attributeAssertionValidity"));

		Checkbox returnSingleAssertion = new Checkbox(
				msg.getMessage("SAMLEditorGeneralTab.returnSingleAssertion"));
		configBinder.forField(returnSingleAssertion)
				.bind(SAMLServiceConfiguration::isReturnSingleAssertion, SAMLServiceConfiguration::setReturnSingleAssertion);
		advancedLayout.addFormItem(returnSingleAssertion, "");

		AccordionPanel accordionPanel = new AccordionPanel(msg.getMessage("SAMLEditorGeneralTab.advanced"),
				advancedLayout);
		accordionPanel.setWidthFull();
		return accordionPanel;
	}

	private AccordionPanel buildMetadataSection()
	{
		FormLayout metadataPublishing = new FormLayout();
		metadataPublishing.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		metadataPublishing.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		Checkbox publishMetadata = new Checkbox(msg.getMessage("SAMLEditorGeneralTab.publishMetadata"));
		configBinder.forField(publishMetadata).withValidator((v, c) -> {
			if (!initialValidation)
			{
				refreshMetaButton(v);
				initialValidation = true;
			}
			if (v)
			{
				metaLinkButtonWrapper.setVisible(true);
				metaOffInfo.getParent().get().setVisible(false);
			} else
			{
				metaLinkButtonWrapper.setVisible(false);
				metaOffInfo.getParent().get().setVisible(true);
			}

			return ValidationResult.ok();

		}).bind(SAMLServiceConfiguration::isPublishMetadata, SAMLServiceConfiguration::setPublishMetadata);
		metadataPublishing.addFormItem(publishMetadata, "");

		signMetadata = new Checkbox(msg.getMessage("SAMLEditorGeneralTab.signMetadata"));
		configBinder.forField(signMetadata)
				.bind(SAMLServiceConfiguration::isSignMetadata, SAMLServiceConfiguration::setSignMetadata);
		signMetadata.setEnabled(false);
		metadataPublishing.addFormItem(signMetadata, "");

		Checkbox autoGenerateMetadata = new Checkbox(
				msg.getMessage("SAMLEditorGeneralTab.autoGenerateMetadata"));
		configBinder.forField(autoGenerateMetadata)
				.bind(SAMLServiceConfiguration::isAutoGenerateMetadata, SAMLServiceConfiguration::setAutoGenerateMetadata);
		autoGenerateMetadata.setEnabled(false);
		metadataPublishing.addFormItem(autoGenerateMetadata, "");

		FileField metadataSource = new FileField(msg, "text/xml", "metadata.xml",
				serverConfig.getFileSizeLimit());
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
							msg.getMessage("SAMLEditorGeneralTab.invalidMetadataFile"));
				}
			}

			boolean isEmpty = value == null || (value.getLocal() == null
					&& (value.getSrc() == null || value.getSrc().isEmpty()));

			if (publishMetadata.getValue() && (!autoGenerateMetadata.getValue() && isEmpty))
			{
				return ValidationResult.error(msg.getMessage("SAMLEditorGeneralTab.idpMetaEmpty"));
			}

			return ValidationResult.ok();

		}));
		metadataSource.setEnabled(false);
		metadataPublishing.addFormItem(metadataSource, msg.getMessage("SAMLEditorGeneralTab.metadataFile"));
		publishMetadata.addValueChangeListener(e -> {
			boolean v = e.getValue();
			signMetadata.setEnabled(v);
			autoGenerateMetadata.setEnabled(v);
			metadataSource.setEnabled(!autoGenerateMetadata.getValue() && v);
		});

		autoGenerateMetadata.addValueChangeListener(e -> {
			metadataSource.setEnabled(!e.getValue() && publishMetadata.getValue());
		});

		AccordionPanel accordionPanel = new AccordionPanel(msg.getMessage("SAMLEditorGeneralTab.metadata"),
				metadataPublishing);
		accordionPanel.setWidthFull();
		return accordionPanel;
	}
	
	private void refreshMetaButton(Boolean enabled)
	{
		metaLinkButton.setEnabled(enabled);
	}

	private AccordionPanel buildIdenityTypeMappingSection()
	{
		VerticalLayout idTypeMappingLayout = new VerticalLayout();
		idTypeMappingLayout.setMargin(false);

		EditableGrid<SAMLIdentityMapping> idMappings = new EditableGrid<>(msg::getMessage, SAMLIdentityMapping::new);
		idTypeMappingLayout.add(idMappings);
		idMappings.addComboBoxColumn(SAMLIdentityMapping::getUnityId, SAMLIdentityMapping::setUnityId,
				idTypes.stream().map(IdentityType::getName).collect(Collectors.toList()))
				.setHeader(msg.getMessage("SAMLEditorGeneralTab.idMappings.unityId"))
				.setAutoWidth(true)
				.setFlexGrow(2);
		idMappings.addColumn(SAMLIdentityMapping::getSamlId, SAMLIdentityMapping::setSamlId, true)
				.setHeader(msg.getMessage("SAMLEditorGeneralTab.idMappings.samlId"))
				.setAutoWidth(true)
				.setFlexGrow(2);

		idMappings.setWidthFull();
		configBinder.forField(idMappings)
				.bind(SAMLServiceConfiguration::getIdentityMapping, SAMLServiceConfiguration::setIdentityMapping);

		AccordionPanel accordionPanel = new AccordionPanel(msg.getMessage("SAMLEditorGeneralTab.idenityTypeMapping"),
				idTypeMappingLayout);
		accordionPanel.setWidthFull();
		return accordionPanel;
	}
	
	private ValidationResult validatePathForAdd(String path)
	{
		if (path == null || path.isEmpty())
		{
			return ValidationResult.error(msg.getMessage("fieldRequired"));
		}

		if (usedEndpointsPaths.contains(path))
		{
			return ValidationResult.error(msg.getMessage("ServiceEditorBase.usedContextPath"));
		}

		try
		{
			EndpointPathValidator.validateEndpointPath(path, serverContextPaths);

		} catch (WrongArgumentException e)
		{
			return ValidationResult.error(msg.getMessage("ServiceEditorBase.invalidContextPath"));
		}

		return ValidationResult.ok();
	}
	
	private ValidationResult validatePathForEdit(String path)
	{
		try
		{
			EndpointPathValidator.validateEndpointPath(path);

		} catch (WrongArgumentException e)
		{
			return ValidationResult.error(msg.getMessage("ServiceEditorBase.invalidContextPath"));
		}

		return ValidationResult.ok();
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.COGS;
	}

	@Override
	public String getType()
	{
		return ServiceEditorComponent.ServiceEditorTab.GENERAL.toString();
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("ServiceEditorBase.general");
	}

}
