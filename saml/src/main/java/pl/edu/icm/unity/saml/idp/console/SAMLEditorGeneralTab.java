/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;

import io.imunity.tooltip.TooltipExtension;
import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import org.vaadin.risto.stepper.IntStepper;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointPathValidator;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.saml.console.SAMLIdentityMapping;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration.AssertionSigningPolicy;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration.RequestAcceptancePolicy;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration.ResponseSigningPolicy;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.*;
import pl.edu.icm.unity.webui.common.file.FileField;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.common.widgets.DescriptionTextField;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorDocument;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SAML service editor general tab
 * 
 * @author P.Piernik
 *
 */
public class SAMLEditorGeneralTab extends CustomComponent implements EditorTab
{
	private MessageSource msg;
	private Binder<DefaultServiceDefinition> samlServiceBinder;
	private Binder<SAMLServiceConfiguration> configBinder;
	private OutputTranslationProfileFieldFactory profileFieldFactory;
	private UnityServerConfiguration serverConfig;
	private SubViewSwitcher subViewSwitcher;
	private Set<String> credentials;
	private Set<String> truststores;
	private List<String> usedEndpointsPaths;
	private Set<String> serverContextPaths;
	private String serverPrefix;
	private Collection<IdentityType> idTypes;
	private boolean editMode;
	private CheckBox signMetadata;
	private HorizontalLayout infoLayout;
	private boolean initialValidation;
	private Button metaLinkButton;
	private HorizontalLayout metaLinkButtonWrapper;
	private Label metaOffInfo;
	
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
		
		setCaption(msg.getMessage("ServiceEditorBase.general"));
		setIcon(Images.cogs.getResource());
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(buildHeaderSection());
		main.addComponent(buildMetadataSection());
		main.addComponent(buildAdvancedSection());
		main.addComponent(buildIdenityTypeMappingSection());
		main.addComponent(profileFieldFactory.getWrappedFieldInstance(subViewSwitcher, configBinder,
				"translationProfile"));
		setCompositionRoot(main);
	}

	private Component buildHeaderSection()
	{
		HorizontalLayout main = new HorizontalLayout();
		main.setMargin(new MarginInfo(true, false));

		FormLayoutWithFixedCaptionWidth mainGeneralLayout = new FormLayoutWithFixedCaptionWidth();
		main.addComponent(mainGeneralLayout);

		metaLinkButton = new Button();
		metaOffInfo = new Label();
		metaOffInfo.setCaption(msg.getMessage("SAMLEditorGeneralTab.metadataOff"));
		
		infoLayout = new HorizontalLayout();
		infoLayout.setMargin(new MarginInfo(false, true, false, true));
		infoLayout.setStyleName("u-marginLeftMinus30");
		infoLayout.addStyleName("u-border");
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		infoLayout.addComponent(wrapper);
		wrapper.addComponent(new Label(msg.getMessage("SAMLEditorGeneralTab.importantURLs")));
		FormLayout infoLayoutWrapper = new FormLayout();
		infoLayoutWrapper.setSpacing(false);
		infoLayoutWrapper.setMargin(false);
		wrapper.addComponent(infoLayoutWrapper);
		metaLinkButtonWrapper = new HorizontalLayout();
		metaLinkButtonWrapper.setCaption(msg.getMessage("SAMLEditorGeneralTab.metadataLink"));
		metaLinkButton.setStyleName(Styles.vButtonLink.toString());
		metaLinkButtonWrapper.addComponent(metaLinkButton);
		infoLayoutWrapper.addComponent(metaLinkButtonWrapper);
		infoLayoutWrapper.addComponent(metaOffInfo);
		metaOffInfo.setVisible(false);
		metaLinkButton.addClickListener(e -> {
			Page.getCurrent().open(metaLinkButton.getCaption(), "_blank", false);
		});
		main.addComponent(infoLayout);
		refreshMetaButton(false);
				
		TextField name = new TextField();
		name.setCaption(msg.getMessage("ServiceEditorBase.name"));
		name.setReadOnly(editMode);
		samlServiceBinder.forField(name).asRequired().bind("name");
		mainGeneralLayout.addComponent(name);

		TextField contextPath = new TextField();
		contextPath.setPlaceholder("/saml-idp");
		contextPath.setRequiredIndicatorVisible(true);
		contextPath.setCaption(msg.getMessage("SAMLEditorGeneralTab.contextPath"));
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
				metaLinkButton.setCaption(serverPrefix + v + "/metadata");
			}
			
			return r;
		}).bind("address");
		mainGeneralLayout.addComponent(contextPath);

		I18nTextField displayedName = new I18nTextField(msg);
		displayedName.setCaption(msg.getMessage("ServiceEditorBase.displayedName"));
		samlServiceBinder.forField(displayedName).bind("displayedName");
		mainGeneralLayout.addComponent(displayedName);

		TextField description = new DescriptionTextField(msg);
		samlServiceBinder.forField(description).bind("description");
		mainGeneralLayout.addComponent(description);

		TextField issuerURI = new TextField();
		issuerURI.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		issuerURI.setCaption(msg.getMessage("SAMLEditorGeneralTab.issuerURI"));
		configBinder.forField(issuerURI).asRequired().bind("issuerURI");
		mainGeneralLayout.addComponent(issuerURI);

		ComboBox<AssertionSigningPolicy> signAssertionPolicy = new ComboBox<>();
		signAssertionPolicy.setItems(AssertionSigningPolicy.values());
		signAssertionPolicy.setEmptySelectionAllowed(false);
		signAssertionPolicy.setCaption(msg.getMessage("SAMLEditorGeneralTab.signAssertionPolicy"));
		configBinder.forField(signAssertionPolicy).asRequired().bind("signAssertionPolicy");
		mainGeneralLayout.addComponent(signAssertionPolicy);

		ComboBox<ResponseSigningPolicy> signResponcePolicy = new ComboBox<>();
		signResponcePolicy.setItems(ResponseSigningPolicy.values());
		signResponcePolicy.setEmptySelectionAllowed(false);
		signResponcePolicy.setCaption(msg.getMessage("SAMLEditorGeneralTab.signResponcePolicy"));
		configBinder.forField(signResponcePolicy).asRequired().bind("signResponcePolicy");
		mainGeneralLayout.addComponent(signResponcePolicy);

		ComboBox<String> signResponseCredential = new ComboBox<>();
		signResponseCredential.setCaption(msg.getMessage("SAMLEditorGeneralTab.signResponseCredential"));
		signResponseCredential.setItems(credentials);
		configBinder.forField(signResponseCredential)
				.asRequired((v, c) -> ((v == null || v.isEmpty()))
						? ValidationResult.error(msg.getMessage("fieldRequired"))
						: ValidationResult.ok())
				.bind("signResponseCredential");
		mainGeneralLayout.addComponent(signResponseCredential);

		ComboBox<String> additionallyAdvertisedCredential = new ComboBox<>();
		additionallyAdvertisedCredential.setCaption(msg.getMessage("SAMLEditorGeneralTab.additionallyAdvertisedCredential"));
		additionallyAdvertisedCredential.setDescription(msg.getMessage("SAMLEditorGeneralTab.additionallyAdvertisedCredentialDesc"));
		TooltipExtension.tooltip(additionallyAdvertisedCredential);
		additionallyAdvertisedCredential.setItems(credentials);
		configBinder.forField(additionallyAdvertisedCredential)
				.bind("additionallyAdvertisedCredential");
		mainGeneralLayout.addComponent(additionallyAdvertisedCredential);
		
		ComboBox<String> httpsTruststore = new ComboBox<>(
				msg.getMessage("SAMLEditorGeneralTab.httpsTruststore"));
		httpsTruststore.setItems(truststores);
		configBinder.forField(httpsTruststore).bind("httpsTruststore");
		mainGeneralLayout.addComponent(httpsTruststore);

		CheckBox skipConsentScreen = new CheckBox(msg.getMessage("SAMLEditorGeneralTab.skipConsentScreen"));
		configBinder.forField(skipConsentScreen).bind("skipConsentScreen");
		mainGeneralLayout.addComponent(skipConsentScreen);

		CheckBox editableConsentScreen = new CheckBox(
				msg.getMessage("SAMLEditorGeneralTab.editableConsentScreen"));
		configBinder.forField(editableConsentScreen).bind("editableConsentScreen");
		mainGeneralLayout.addComponent(editableConsentScreen);

		skipConsentScreen.addValueChangeListener(e -> editableConsentScreen.setEnabled(!e.getValue()));

		ComboBox<RequestAcceptancePolicy> acceptPolicy = new ComboBox<>();
		acceptPolicy.setItems(RequestAcceptancePolicy.values());
		acceptPolicy.setEmptySelectionAllowed(false);
		acceptPolicy.setCaption(msg.getMessage("SAMLEditorGeneralTab.acceptPolicy"));
		configBinder.forField(acceptPolicy).asRequired().bind("requestAcceptancePolicy");
		mainGeneralLayout.addComponent(acceptPolicy);

		CheckBox sendNotBefore = new CheckBox(
				msg.getMessage("SAMLEditorGeneralTab.sendNotBefore"));
		configBinder.forField(sendNotBefore)
				.bind(SAMLServiceConfiguration::isSendNotBeforeConstraint, SAMLServiceConfiguration::setSendNotBeforeConstraint);
		mainGeneralLayout.addComponent(sendNotBefore);

		
		return main;
	}

	private CollapsibleLayout buildAdvancedSection()
	{
		FormLayoutWithFixedCaptionWidth advancedLayout = new FormLayoutWithFixedCaptionWidth();
		advancedLayout.setMargin(false);

		IntStepper authenticationTimeout = new IntStepper();
		authenticationTimeout.setWidth(5, Unit.EM);
		authenticationTimeout.setCaption(msg.getMessage("SAMLEditorGeneralTab.authenticationTimeout"));
		configBinder.forField(authenticationTimeout).asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("authenticationTimeout");
		advancedLayout.addComponent(authenticationTimeout);

		IntStepper requestValidity = new IntStepper();
		requestValidity.setWidth(5, Unit.EM);
		requestValidity.setCaption(msg.getMessage("SAMLEditorGeneralTab.requestValidity"));
		configBinder.forField(requestValidity).asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("requestValidity");
		advancedLayout.addComponent(requestValidity);

		IntStepper attrAssertionValidity = new IntStepper();
		attrAssertionValidity.setWidth(5, Unit.EM);
		attrAssertionValidity.setCaption(msg.getMessage("SAMLEditorGeneralTab.attributeAssertionValidity"));
		configBinder.forField(attrAssertionValidity).asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("attrAssertionValidity");
		advancedLayout.addComponent(attrAssertionValidity);

		CheckBox returnSingleAssertion = new CheckBox(
				msg.getMessage("SAMLEditorGeneralTab.returnSingleAssertion"));
		configBinder.forField(returnSingleAssertion).bind("returnSingleAssertion");
		advancedLayout.addComponent(returnSingleAssertion);

		return new CollapsibleLayout(msg.getMessage("SAMLEditorGeneralTab.advanced"), advancedLayout);
	}

	private CollapsibleLayout buildMetadataSection()
	{
		FormLayoutWithFixedCaptionWidth metadataPublishing = new FormLayoutWithFixedCaptionWidth();
		metadataPublishing.setMargin(false);

		CheckBox publishMetadata = new CheckBox(msg.getMessage("SAMLEditorGeneralTab.publishMetadata"));
		configBinder.forField(publishMetadata).withValidator((v, c) -> {
			if (!initialValidation)
			{
				refreshMetaButton(v);
				initialValidation = true;
			}
			if (v)
			{
				metaLinkButtonWrapper.setVisible(true);
				metaOffInfo.setVisible(false);
			} else
			{
				metaLinkButtonWrapper.setVisible(false);
				metaOffInfo.setVisible(true);
			}

			return ValidationResult.ok();

		}).bind("publishMetadata");
		metadataPublishing.addComponent(publishMetadata);

		signMetadata = new CheckBox(msg.getMessage("SAMLEditorGeneralTab.signMetadata"));
		configBinder.forField(signMetadata).bind("signMetadata");
		signMetadata.setEnabled(false);
		metadataPublishing.addComponent(signMetadata);

		CheckBox autoGenerateMetadata = new CheckBox(
				msg.getMessage("SAMLEditorGeneralTab.autoGenerateMetadata"));
		configBinder.forField(autoGenerateMetadata).bind("autoGenerateMetadata");
		autoGenerateMetadata.setEnabled(false);
		metadataPublishing.addComponent(autoGenerateMetadata);

		FileField metadataSource = new FileField(msg, "text/xml", "metadata.xml",
				serverConfig.getFileSizeLimit());
		metadataSource.setCaption(msg.getMessage("SAMLEditorGeneralTab.metadataFile"));
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
					&& (value.getRemote() == null || value.getRemote().isEmpty()));

			if (publishMetadata.getValue() && (!autoGenerateMetadata.getValue() && isEmpty))
			{
				return ValidationResult.error(msg.getMessage("SAMLEditorGeneralTab.idpMetaEmpty"));
			}

			return ValidationResult.ok();

		}));
		metadataSource.setEnabled(false);
		metadataPublishing.addComponent(metadataSource);
		publishMetadata.addValueChangeListener(e -> {
			boolean v = e.getValue();
			signMetadata.setEnabled(v);
			autoGenerateMetadata.setEnabled(v);
			metadataSource.setEnabled(!autoGenerateMetadata.getValue() && v);
		});

		autoGenerateMetadata.addValueChangeListener(e -> {
			metadataSource.setEnabled(!e.getValue() && publishMetadata.getValue());
		});

		return new CollapsibleLayout(msg.getMessage("SAMLEditorGeneralTab.metadata"), metadataPublishing);
	}
	
	private void refreshMetaButton(Boolean enabled)
	{
		metaLinkButton.setEnabled(enabled);
		if (!enabled)
		{
			metaLinkButton.addStyleName(Styles.disabledButton.toString());
		} else
		{
			metaLinkButton.removeStyleName(Styles.disabledButton.toString());
		}
	}

	private CollapsibleLayout buildIdenityTypeMappingSection()
	{
		VerticalLayout idTypeMappingLayout = new VerticalLayout();
		idTypeMappingLayout.setMargin(false);

		GridWithEditor<SAMLIdentityMapping> idMappings = new GridWithEditor<>(msg, SAMLIdentityMapping.class);
		idTypeMappingLayout.addComponent(idMappings);
		idMappings.addComboColumn(s -> s.getUnityId(), (t, v) -> t.setUnityId(v),
				msg.getMessage("SAMLEditorGeneralTab.idMappings.unityId"),
				idTypes.stream().map(t -> t.getName()).collect(Collectors.toList()), 30, false);
		idMappings.addTextColumn(s -> s.getSamlId(), (t, v) -> t.setSamlId(v),
				msg.getMessage("SAMLEditorGeneralTab.idMappings.samlId"), 70, false);

		idMappings.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(idMappings).bind("identityMapping");

		return new CollapsibleLayout(msg.getMessage("SAMLEditorGeneralTab.idenityTypeMapping"),
				idTypeMappingLayout);
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
	public String getType()
	{
		return ServiceEditorTab.GENERAL.toString();
	}

	@Override
	public CustomComponent getComponent()
	{
		return this;
	}

}
