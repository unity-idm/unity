/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;


import static io.imunity.console.tprofile.Constants.FORM_PROFILE;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import io.imunity.console.tprofile.ActionParameterComponentProvider;
import io.imunity.console.tprofile.RegistrationTranslationProfileEditor;
import io.imunity.vaadin.auth.services.idp.PolicyAgreementConfigurationList;
import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.elements.LocalizedTextAreaDetails;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.NotEmptyComboBox;
import io.imunity.vaadin.elements.NotEmptyIntegerField;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.base.registration.ExternalSignupSpec;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationFormLayouts;
import pl.edu.icm.unity.base.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.base.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;


@PrototypeComponent
public class RegistrationFormEditor extends BaseFormEditor
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RegistrationFormEditor.class);
	private final MessageSource msg;
	private final GroupsManagement groupsMan;
	private final NotificationsManagement notificationsMan;
	private final MessageTemplateManagement msgTempMan;
	private final CredentialRequirementManagement credReqMan;
	private final AuthenticatorSupportService authenticatorSupport;
	private final RealmsManagement realmsManagement;
	private final FileStorageService fileStorageService;
	private final VaadinLogoImageLoader imageAccessService;
	private final UnityServerConfiguration serverConfig;
	private final NotificationPresenter notificationPresenter;
	private final HtmlTooltipFactory htmlTooltipFactory;

	private TabSheet tabs;
	private Checkbox ignoreRequestsAndInvitation;

	private Checkbox publiclyAvailable;
	private Checkbox byInvitationOnly;
	private RegistrationFormNotificationsEditor notificationsEditor;
	private IntegerField captcha;

	private LocalizedTextFieldDetails title2ndStage;
	private LocalizedTextAreaDetails formInformation2ndStage;
	private Checkbox showGotoSignin;
	private TextField signInUrl;
	private TextField registrationCode;
	private RemoteAuthnProvidersMultiSelection remoteAuthnSelections;
	private LocalizedTextAreaDetails switchToEnquiryInfo;


	private RemoteAuthnProvidersMultiSelection remoteAuthnGridSelections;
	private IntegerField remoteAuthnGridHeight;
	private Checkbox remoteAuthnGridSearchable;

	private NotEmptyComboBox<String> credentialRequirementAssignment;
	private final RegistrationActionsRegistry actionsRegistry;
	private RegistrationTranslationProfileEditor profileEditor;
	private RegistrationFormLayoutSettingsEditor layoutSettingsEditor;
	private RegistrationFormLayoutEditor layoutEditor;
	private Checkbox showCancel;
	private Checkbox localSignupEmbeddedAsButton;
	private ComboBox<String> realmNames;

	RegistrationFormEditor(MessageSource msg, UnityServerConfiguration serverConfig,
			GroupsManagement groupsMan, NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan, IdentityTypeSupport identitiesMan,
			AttributeTypeManagement attributeMan, CredentialManagement credMan,
			RegistrationActionsRegistry actionsRegistry, CredentialRequirementManagement credReqMan,
			ActionParameterComponentProvider actionComponentFactory,
			AuthenticatorSupportService authenticatorSupport, RealmsManagement realmsManagement,
			FileStorageService fileStorageService, URIAccessService uriAccessService,
			VaadinLogoImageLoader imageAccessService,
			PolicyAgreementConfigurationList.PolicyAgreementConfigurationListFactory policyAgreementConfigurationListFactory,
			AttributeTypeSupport attributeTypeSupport, NotificationPresenter notificationPresenter, HtmlTooltipFactory htmlTooltipFactory)
			throws EngineException
	{
		super(msg, identitiesMan, attributeMan, credMan, policyAgreementConfigurationListFactory, attributeTypeSupport, actionComponentFactory);
		this.actionsRegistry = actionsRegistry;
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.notificationsMan = notificationsMan;
		this.msgTempMan = msgTempMan;
		this.credReqMan = credReqMan;
		this.authenticatorSupport = authenticatorSupport;
		this.realmsManagement = realmsManagement;
		this.fileStorageService = fileStorageService;
		this.imageAccessService = imageAccessService;
		this.serverConfig = serverConfig;
		this.notificationPresenter = notificationPresenter;
		this.htmlTooltipFactory = htmlTooltipFactory;
	}

	public RegistrationFormEditor init(boolean copyMode)
			throws EngineException
	{
		this.copyMode = copyMode;
		initUI();
		return this;
	}

	private void initUI() throws EngineException
	{
		setWidthFull();
		setHeightFull();
		setPadding(false);
		tabs = new TabSheet();
		tabs.setWidthFull();
		initMainTab();
		initCollectedTab();
		initDisplaySettingsTab();
		initLayoutTab();
		initWrapUpTab();
		initAssignedTab();
		ignoreRequestsAndInvitation = new Checkbox(
				msg.getMessage("RegistrationFormEditDialog.ignoreRequestsAndInvitations"));
		add(ignoreRequestsAndInvitation);
		ignoreRequestsAndInvitation.getStyle().set("align-self", "end");
		ignoreRequestsAndInvitation.setVisible(false);
		add(tabs);
	}

	public RegistrationForm getForm() throws FormValidationException
	{
		RegistrationFormBuilder builder = getFormBuilderBasic();
		builder.withTranslationProfile(profileEditor.getProfile());
		RegistrationFormNotifications notCfg = notificationsEditor.getValue();
		builder.withNotificationsConfiguration(notCfg);
		RegistrationFormLayouts layouts = layoutEditor.getLayouts();
		layouts.setLocalSignupEmbeddedAsButton(localSignupEmbeddedAsButton.getValue());
		builder.withLayouts(layouts);
		builder.withAutoLoginToRealm(realmNames.getValue());

		try
		{
			RegistrationForm form = builder.build();
			form.validateLayouts();
			return form;
		} catch (Exception e)
		{
			throw new FormValidationException(e.getMessage(), e);
		}
	}

	private RegistrationFormBuilder getFormBuilderBasic() throws FormValidationException
	{
		RegistrationFormBuilder builder = new RegistrationFormBuilder();
		super.buildCommon(builder);

		builder.withDefaultCredentialRequirement(credentialRequirementAssignment.getValue());
		builder.withCaptchaLength(captcha.getValue() == null ? 0 : captcha.getValue());
		builder.withPubliclyAvailable(publiclyAvailable.getValue());
		builder.withByInvitationOnly(byInvitationOnly.getValue());
		String code = registrationCode.getValue();
		if (code != null && !code.equals(""))
			builder.withRegistrationCode(code);
		builder.withExternalSignupSpec(new ExternalSignupSpec(remoteAuthnSelections.getSelectedItems().stream().toList()));
		builder.withExternalGridSignupSpec(
				new ExternalSignupGridSpec(remoteAuthnGridSelections.getSelectedItems().stream().toList(),
						new ExternalSignupGridSpec.AuthnGridSettings(remoteAuthnGridSearchable.getValue(),
								remoteAuthnGridHeight.getValue())));
		FormLayoutSettings settings = layoutSettingsEditor.getSettings(builder.getName());
		settings.setShowCancel(showCancel.getValue());
		builder.withFormLayoutSettings(settings);
		builder.withTitle2ndStage(new I18nString(title2ndStage.getValue()));
		builder.withFormInformation2ndStage(new I18nString(formInformation2ndStage.getValue()));
		builder.withSwitchToEnquiryInfo(new I18nString(switchToEnquiryInfo.getValue()));
		builder.withShowGotoSignIn(showGotoSignin.getValue(), signInUrl.getValue());
		RegistrationFormLayouts layouts = new RegistrationFormLayouts(); //FIXME
		layouts.setLocalSignupEmbeddedAsButton(localSignupEmbeddedAsButton.getValue());
		builder.withLayouts(layouts);
		return builder;
	}

	public void setForm(RegistrationForm toEdit)
	{
		super.setValue(toEdit);
		publiclyAvailable.setValue(toEdit.isPubliclyAvailable());
		byInvitationOnly.setValue(toEdit.isByInvitationOnly());
		RegistrationFormNotifications notCfg = toEdit.getNotificationsConfiguration();
		notificationsEditor.setValue(notCfg);
		captcha.setValue(toEdit.getCaptchaLength());
		if (toEdit.getRegistrationCode() != null)
			registrationCode.setValue(toEdit.getRegistrationCode());
		if (toEdit.getTitle2ndStage() != null)
			title2ndStage.setValue(toEdit.getTitle2ndStage().getLocalizedMap());
		if (toEdit.getFormInformation2ndStage() != null)
			formInformation2ndStage.setValue(toEdit.getFormInformation2ndStage().getLocalizedMap());
		credentialRequirementAssignment.setValue(toEdit.getDefaultCredentialRequirement());
		TranslationProfile profile = new TranslationProfile(
				toEdit.getTranslationProfile().getName(), "",
				ProfileType.REGISTRATION,
				toEdit.getTranslationProfile().getRules());
		profileEditor.setValue(profile);
		layoutSettingsEditor.setSettings(toEdit.getLayoutSettings());
		layoutEditor.setFormLayouts(toEdit.getFormLayouts());
		showGotoSignin.setValue(toEdit.isShowSignInLink());
		signInUrl.setValue(toEdit.getSignInLink() == null ? "" : toEdit.getSignInLink());
		signInUrl.setEnabled(showGotoSignin.getValue());
		if (!copyMode)
			ignoreRequestsAndInvitation.setVisible(true);

		remoteAuthnSelections.setValue(toEdit.getExternalSignupSpec().getSpecs());
		remoteAuthnGridSelections.setItems(remoteAuthnSelections.getSelectedItems());
		remoteAuthnGridSelections.setValue(toEdit.getExternalSignupGridSpec().getSpecs());
		showCancel.setValue(toEdit.getLayoutSettings().isShowCancel());
		localSignupEmbeddedAsButton.setValue(toEdit.getFormLayouts().isLocalSignupEmbeddedAsButton());
		realmNames.setValue(toEdit.getAutoLoginToRealm() == null ? "" : toEdit.getAutoLoginToRealm());
		ExternalSignupGridSpec.AuthnGridSettings gsettings = toEdit.getExternalSignupGridSpec().getGridSettings();
		if (gsettings == null)
			gsettings = new ExternalSignupGridSpec.AuthnGridSettings();
		remoteAuthnGridSearchable.setValue(gsettings.searchable);
		remoteAuthnGridHeight.setValue(gsettings.height);
		switchToEnquiryInfo.setValue(toEdit.getSwitchToEnquiryInfoFallbackToDefault(msg).getLocalizedMap());

		refreshRemoteAuthGridSettingsControls();
	}

	private void initMainTab() throws EngineException
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		tabs.add(msg.getMessage("RegistrationFormViewer.mainTab"), main);

		initNameAndDescFields(msg.getMessage("RegistrationFormEditor.defaultName"));

		publiclyAvailable = new Checkbox(msg.getMessage("RegistrationFormEditor.publiclyAvailable"));
		publiclyAvailable.addValueChangeListener(event -> {
			boolean isPublic = publiclyAvailable.getValue();
			if (!isPublic)
				byInvitationOnly.setValue(false);
			byInvitationOnly.setEnabled(isPublic);
		});

		byInvitationOnly = new Checkbox(msg.getMessage("RegistrationFormEditor.byInvitationOnly"));
		byInvitationOnly.setEnabled(false);

		main.addFormItem(name, msg.getMessage("RegistrationFormEditor.name"));
		main.addFormItem(description, msg.getMessage("descriptionF"));
		main.addFormItem(publiclyAvailable, "");
		main.addFormItem(byInvitationOnly, "");

		notificationsEditor = new RegistrationFormNotificationsEditor(msg, groupsMan,
				notificationsMan, msgTempMan);
		notificationsEditor.addToFormLayout(main);

		realmNames = new ComboBox<>();
		realmNames.setItems(realmsManagement.getRealms().stream().map(AuthenticationRealm::getName).toList());
		main.addFormItem(realmNames, msg.getMessage("RegistrationFormEditor.autoLoginAutoAcceptedToRealm"))
				.add(htmlTooltipFactory.get(msg.getMessage("RegistrationFormEditor.autoLoginAutoAcceptedToRealm.description")));

		captcha = new NotEmptyIntegerField();
		captcha.setMin(0);
		captcha.setMax(RegistrationForm.MAX_CAPTCHA_LENGTH);
		captcha.setStepButtonsVisible(true);

		main.addFormItem(captcha, msg.getMessage("RegistrationFormViewer.captcha"))
				.add(htmlTooltipFactory.get(msg.getMessage("RegistrationFormEditor.captchaDescription")));
	}

	private void initWrapUpTab()
	{
		tabs.add(msg.getMessage("RegistrationFormEditor.wrapUpTab"), getWrapUpComponent(
				RegistrationWrapUpConfig.TriggeringState::isSuitableForRegistration));
	}

	private void initCollectedTab() throws EngineException
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		collectComments = new Checkbox(msg.getMessage("RegistrationFormEditor.collectComments"));
		registrationCode = new TextField();
		main.addFormItem(registrationCode, msg.getMessage("RegistrationFormViewer.registrationCode"));
		main.addFormItem(collectComments, "");
		main.addFormItem(checkIdentityOnSubmit, "");

		TabSheet tabOfLists = createCollectedParamsTabs(notificationsEditor.getGroups(), false);
		Component remoteSignUpMetnodsTab = createRemoteSignupMethodsTab();
		tabOfLists.add(new Tab(msg.getMessage("RegistrationFormEditor.remoteSignupMethods")), remoteSignUpMetnodsTab, 1);
		tabOfLists.setSelectedIndex(0);

		VerticalLayout wrapper = new VerticalLayout(main, tabOfLists);
		wrapper.setPadding(false);
		tabs.add(msg.getMessage("RegistrationFormViewer.collectedTab"), wrapper);
	}

	private void initDisplaySettingsTab()
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		initCommonDisplayedFields();
		title2ndStage = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		title2ndStage.setWidth(TEXT_FIELD_MEDIUM.value());
		formInformation.setWidth(TEXT_FIELD_BIG.value());
		formInformation2ndStage = new LocalizedTextAreaDetails(msg.getEnabledLocales().values(), msg.getLocale());
		formInformation2ndStage.setWidth(TEXT_FIELD_BIG.value());
		showGotoSignin = new Checkbox(msg.getMessage("RegistrationFormViewer.showGotoSignin"));
		showGotoSignin.addValueChangeListener(v -> signInUrl.setEnabled(showGotoSignin.getValue()));
		signInUrl = new TextField();
		signInUrl.setWidthFull();
		signInUrl.setEnabled(false);
		showCancel = new Checkbox(msg.getMessage("FormLayoutEditor.showCancel"));
		localSignupEmbeddedAsButton = new Checkbox(msg.getMessage("FormLayoutEditor.localSignupEmbeddedAsButton"));
		switchToEnquiryInfo = new LocalizedTextAreaDetails(msg.getEnabledLocales().values(), msg.getLocale());
		switchToEnquiryInfo.setValue(RegistrationForm.getDefaultSwitchToEnquiryInfo(msg).getLocalizedMap());
		switchToEnquiryInfo.setWidth(TEXT_FIELD_BIG.value());

		main.addFormItem(displayedName, msg.getMessage("RegistrationFormViewer.displayedName"));
		main.addFormItem(title2ndStage, msg.getMessage("RegistrationFormViewer.title2ndStage"));
		main.addFormItem(formInformation, msg.getMessage("RegistrationFormViewer.formInformation"));
		main.addFormItem(formInformation2ndStage, msg.getMessage("RegistrationFormViewer.formInformation2ndStage"));
		Component icon = htmlTooltipFactory.get(msg.getMessage("RegistrationFormEditor.switchToEnquiryInfo.tip"));
		icon.getStyle().set("margin-top", CSSVars.BASE_MARGIN.value());
		main.addFormItem(switchToEnquiryInfo, msg.getMessage("RegistrationFormEditor.switchToEnquiryInfo"))
				.add(icon);
		main.addFormItem(pageTitle, msg.getMessage("RegistrationFormEditor.registrationPageTitle"));
		main.addFormItem(showGotoSignin, "");
		main.addFormItem(signInUrl, msg.getMessage("RegistrationFormEditor.signinURL"));
		main.addFormItem(showCancel, "");
		main.addFormItem(localSignupEmbeddedAsButton, "");

		layoutSettingsEditor = new RegistrationFormLayoutSettingsEditor(msg, serverConfig, fileStorageService,
				imageAccessService, htmlTooltipFactory);
		VerticalLayout wrapper = new VerticalLayout(main, layoutSettingsEditor);
		wrapper.setPadding(false);
		wrapper.setSpacing(false);
		tabs.add(msg.getMessage("RegistrationFormViewer.displayTab"), wrapper);
	}

	private Component createRemoteSignupMethodsTab() throws EngineException
	{
		remoteAuthnSelections = new RemoteAuthnProvidersMultiSelection(msg, authenticatorSupport);
		remoteAuthnSelections.setWidth(TEXT_FIELD_BIG.value());
		remoteAuthnGridSelections = new RemoteAuthnProvidersMultiSelection(msg);
		remoteAuthnGridSelections.setWidth(TEXT_FIELD_BIG.value());

		remoteAuthnSelections.addValueChangeListener(e ->
		{
			Set<AuthenticationOptionsSelector> values = e.getValue();
			Set<AuthenticationOptionsSelector> selectedItems = remoteAuthnGridSelections.getSelectedItems();
			remoteAuthnGridSelections.setItems(values);
			remoteAuthnGridSelections.setValue(selectedItems.stream()
					.filter(values::contains)
					.collect(Collectors.toList()));
		});

		remoteAuthnGridSelections.addValueChangeListener(e -> refreshRemoteAuthGridSettingsControls());
		remoteAuthnGridSelections.addSelectionListener(e -> refreshRemoteAuthGridSettingsControls());

		remoteAuthnGridSearchable = new Checkbox(msg.getMessage("RegistrationFormEditor.remoteAuthEnableGridSearch"));
		remoteAuthnGridHeight = new NotEmptyIntegerField();
		remoteAuthnGridHeight.setValue(5);
		remoteAuthnGridHeight.setMin(1);
		remoteAuthnGridHeight.setMax(100);
		remoteAuthnGridHeight.setStepButtonsVisible(true);
		refreshRemoteAuthGridSettingsControls();

		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		main.addFormItem(remoteAuthnSelections, msg.getMessage("RegistrationFormEditor.remoteAuthenOptions"))
				.add(htmlTooltipFactory.get(msg.getMessage("RegistrationFormEditor.remoteAuthenOptions.description")));
		main.addFormItem(remoteAuthnGridSelections, msg.getMessage("RegistrationFormEditor.remoteAuthenGridOptions"))
				.add(htmlTooltipFactory.get(msg.getMessage("RegistrationFormEditor.remoteAuthenGridOptions.description")));
		main.addFormItem(remoteAuthnGridSearchable, "");
		main.addFormItem(remoteAuthnGridHeight, msg.getMessage("RegistrationFormEditor.remoteAuthGridHeight"));

		return main;
	}

	private void refreshRemoteAuthGridSettingsControls()
	{
		boolean enabled = remoteAuthnGridSelections.getSelectedItems().size() > 0;
		remoteAuthnGridHeight.setEnabled(enabled);
		remoteAuthnGridSearchable.setEnabled(enabled);
	}

	private void initLayoutTab()
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setPadding(false);
		layoutEditor = new RegistrationFormLayoutEditor(msg, this::formProvider, notificationPresenter);
		wrapper.add(layoutEditor);
		tabs.addSelectedChangeListener(event -> layoutEditor.updateFromForm());
		tabs.add(msg.getMessage("RegistrationFormViewer.layoutTab"), wrapper);
	}

	private RegistrationForm formProvider()
	{
		try
		{
			return getFormBuilderBasic().build();
		} catch (Exception e)
		{
			log.debug("Ignoring layout update, form is invalid", e);
			return null;
		}
	}

	private void initAssignedTab() throws EngineException
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setPadding(false);
		tabs.add(msg.getMessage("RegistrationFormViewer.assignedTab"), wrapper);

		credentialRequirementAssignment = new NotEmptyComboBox<>();
		List<String> credentialReqirementNames = credReqMan.getCredentialRequirements().stream()
				.map(CredentialRequirements::getName)
				.collect(Collectors.toList());
		credentialRequirementAssignment.setItems(credentialReqirementNames);
		credentialRequirementAssignment.setValue(credentialReqirementNames.iterator().next());

		profileEditor = new RegistrationTranslationProfileEditor(msg, actionsRegistry, actionComponentProvider, notificationPresenter, htmlTooltipFactory);
		profileEditor.setValue(new TranslationProfile(FORM_PROFILE, "", ProfileType.REGISTRATION,
				new ArrayList<>()));
		main.addFormItem(credentialRequirementAssignment, msg.getMessage("RegistrationFormViewer.credentialRequirementAssignment"));
		wrapper.add(profileEditor);
	}

	public boolean isIgnoreRequestsAndInvitations()
	{
		return ignoreRequestsAndInvitation.getValue();
	}

	@Override
	protected void onGroupChanges()
	{
		super.onGroupChanges();
		profileEditor.refresh();
	}
}
