/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.imunity.console.components.TooltipFactory;
import io.imunity.console.tprofile.ActionParameterComponentProvider;
import io.imunity.console.tprofile.RegistrationTranslationProfileEditor;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.elements.EnumComboBox;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.services.idp.PolicyAgreementConfigurationList;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.mvel.MVELExpressionField;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.base.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.*;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.bulkops.EntityMVELContextKey;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.imunity.console.tprofile.Constants.FORM_PROFILE;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

@PrototypeComponent
public class EnquiryFormEditor extends BaseFormEditor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryFormEditor.class);

	private final MessageSource msg;
	private final GroupsManagement groupsMan;
	private final NotificationsManagement notificationsMan;
	private final MessageTemplateManagement msgTempMan;
	private final FileStorageService fileStorageService;
	private final UnityServerConfiguration serverConfig;
	private final NotificationPresenter notificationPresenter;

	private TabSheet tabs;
	private Checkbox ignoreRequestsAndInvitation;

	private EnumComboBox<EnquiryForm.EnquiryType> enquiryType;
	private MultiSelectComboBox<Group> targetGroups;
	private MVELExpressionField targetCondition;
	private EnquiryFormNotificationsEditor notificationsEditor;
	private RegistrationFormLayoutSettingsEditor layoutSettingsEditor;
	private Checkbox byInvitationOnly;

	private final RegistrationActionsRegistry actionsRegistry;
	private RegistrationTranslationProfileEditor profileEditor;
	private EnquiryFormLayoutEditorTab layoutEditor;
	private final VaadinLogoImageLoader imageAccessService;

	//binder is only for targetCondition validation
	private Binder<EnquiryForm> binder;
	private Map<String, Group> allGroups;

	EnquiryFormEditor(MessageSource msg, UnityServerConfiguration serverConfig,
			GroupsManagement groupsMan, NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan, IdentityTypeSupport identitiesMan,
			AttributeTypeManagement attributeMan, CredentialManagement authenticationMan,
			RegistrationActionsRegistry actionsRegistry,
			ActionParameterComponentProvider actionComponentFactory, FileStorageService fileStorageService,
			VaadinLogoImageLoader imageAccessService,
			PolicyAgreementConfigurationList.PolicyAgreementConfigurationListFactory policyAgreementConfigurationListFactory,
			AttributeTypeSupport attributeTypeSupport, NotificationPresenter notificationPresenter)
			throws EngineException
	{
		super(msg, identitiesMan, attributeMan, authenticationMan, policyAgreementConfigurationListFactory,
				attributeTypeSupport, actionComponentFactory);
		this.actionsRegistry = actionsRegistry;
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.notificationsMan = notificationsMan;
		this.msgTempMan = msgTempMan;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.imageAccessService = imageAccessService;
		this.notificationPresenter = notificationPresenter;
	}
	
	public EnquiryFormEditor init(boolean copyMode)
			throws EngineException
	{
		this.copyMode = copyMode;
		this.binder = new Binder<>(EnquiryForm.class);
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
		initDisplayedTab();
		initLayoutTab();
		initWrapUpTab();
		initAssignedTab();
		ignoreRequestsAndInvitation = new Checkbox(
				msg.getMessage("RegistrationFormEditDialog.ignoreRequestsAndInvitations"));
		add(ignoreRequestsAndInvitation);
		ignoreRequestsAndInvitation.getStyle().set("align-self", "end");
		add(tabs);
	}

	public EnquiryForm getForm() throws FormValidationException
	{
		EnquiryFormBuilder builder = getFormBuilderBasic();

		builder.withTranslationProfile(profileEditor.getProfile());
		EnquiryFormNotifications notCfg = notificationsEditor.getValue();
		builder.withNotificationsConfiguration(notCfg);
		FormLayoutSettings settings = layoutSettingsEditor.getSettings(builder.getName());
		builder.withFormLayoutSettings(settings);

		builder.withLayout(layoutEditor.getLayout());
		builder.withByInvitationOnly(byInvitationOnly.getValue());

		EnquiryForm form;
		try
		{
			form = builder.build();
			form.validateLayout();
		} catch (Exception e)
		{
			throw new FormValidationException(e.getMessage(), e);
		}

		if (!binder.isValid())
		{
			throw new FormValidationException();
		}


		return form;
	}

	private EnquiryFormBuilder getFormBuilderBasic() throws FormValidationException
	{
		EnquiryFormBuilder builder = new EnquiryFormBuilder();
		super.buildCommon(builder);

		builder.withType(enquiryType.getValue());
		builder.withTargetGroups(targetGroups.getValue().stream().map(Group::getPathEncoded).toList().toArray(new String[0]));
		builder.withTargetCondition(targetCondition.getValue());
		return builder;
	}

	public void setForm(EnquiryForm toEdit)
	{
		super.setValue(toEdit);
		notificationsEditor.setValue(toEdit.getNotificationsConfiguration());
		enquiryType.setValue(toEdit.getType());
		List<String> groups = Arrays.asList(toEdit.getTargetGroups());
		targetGroups.setValue(allGroups.entrySet().stream()
				.filter(entry -> groups.contains(entry.getKey()))
				.map(Map.Entry::getValue)
				.collect(Collectors.toSet()));
		binder.setBean(toEdit);

		TranslationProfile profile = new TranslationProfile(
				toEdit.getTranslationProfile().getName(), "",
				ProfileType.REGISTRATION,
				toEdit.getTranslationProfile().getRules());
		profileEditor.setValue(profile);
		layoutSettingsEditor.setSettings(toEdit.getLayoutSettings());
		layoutEditor.setLayout(toEdit.getLayout());
		if (!copyMode)
		{
			ignoreRequestsAndInvitation.setVisible(true);
		}
		byInvitationOnly.setValue(toEdit.isByInvitationOnly());
	}

	private void initMainTab() throws EngineException
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setPadding(false);
		tabs.add(msg.getMessage("RegistrationFormViewer.mainTab"), wrapper);

		initNameAndDescFields(msg.getMessage("EnquiryFormEditor.defaultName"));
		main.addFormItem(name, msg.getMessage("RegistrationFormEditor.name"));
		main.addFormItem(description, msg.getMessage("descriptionF"));

		notificationsEditor = new EnquiryFormNotificationsEditor(msg, groupsMan,
				notificationsMan, msgTempMan);
		enquiryType = new EnumComboBox<>(
				msg::getMessage, "EnquiryType.", EnquiryForm.EnquiryType.class,
				EnquiryForm.EnquiryType.REQUESTED_OPTIONAL);
		enquiryType.setWidth(TEXT_FIELD_MEDIUM.value());
		enquiryType.addValueChangeListener(e -> {
			boolean enable = !e.getValue().equals(EnquiryForm.EnquiryType.STICKY);
			setCredentialsTabVisible(enable);
			setIdentitiesTabVisible(enable);
			if (!enable)
			{
				resetCredentialTab();
				resetIdentitiesTab();
			}
		});

		targetGroups = new MultiSelectComboBox<>();
		allGroups = groupsMan.getAllGroups();
		targetGroups.setItems(allGroups.values());
		targetGroups.setRequiredIndicatorVisible(true);
		targetGroups.setWidth(TEXT_FIELD_BIG.value());
		targetGroups.setItemLabelGenerator(group -> group.getDisplayedName().getValue(msg));
		targetGroups.setRenderer(new ComponentRenderer<>(group -> new GroupItemPresentation(group, msg)));

		targetCondition = new MVELExpressionField(msg,
				msg.getMessage("EnquiryFormEditor.targetConditionDesc"),
				MVELExpressionContext.builder()
						.withTitleKey("EnquiryFormEditor.targetConditionTitle")
						.withEvalToKey("MVELExpressionField.evalToBoolean")
						.withVars(EntityMVELContextKey.toMap()).build(),
						new TooltipFactory()
		);
		targetCondition.setWidthFull();
		targetCondition.setWidth(TEXT_FIELD_BIG.value());
		targetCondition.configureBinding(binder, "targetCondition", false);

		byInvitationOnly = new Checkbox(msg.getMessage("RegistrationFormEditor.byInvitationOnly"));

		main.addFormItem(enquiryType, msg.getMessage("EnquiryFormViewer.type"));
		main.addFormItem(byInvitationOnly, "");
		main.addFormItem(targetGroups, msg.getMessage("EnquiryFormViewer.targetGroups"));
		main.addFormItem(targetCondition, msg.getMessage("EnquiryFormEditor.targetCondition"));

		notificationsEditor.addToFormLayout(main);
	}

	private void initCollectedTab() throws EngineException
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		collectComments = new Checkbox(msg.getMessage("RegistrationFormEditor.collectComments"));
		main.add(collectComments);
		main.add(checkIdentityOnSubmit);

		TabSheet tabOfLists = createCollectedParamsTabs(notificationsEditor.getGroups(), true);
		tabOfLists.setWidthFull();
		tabOfLists.setSelectedIndex(2);

		VerticalLayout wrapper = new VerticalLayout(main, tabOfLists);
		wrapper.setPadding(false);
		tabs.add(msg.getMessage("RegistrationFormViewer.collectedTab"), wrapper);
	}

	private void initDisplayedTab()
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		initCommonDisplayedFields();
		main.addFormItem(displayedName, msg.getMessage("RegistrationFormViewer.displayedName"));
		main.addFormItem(formInformation, msg.getMessage("RegistrationFormViewer.formInformation"));
		formInformation.setWidth(TEXT_FIELD_BIG.value());
		main.addFormItem(pageTitle, msg.getMessage("RegistrationFormEditor.registrationPageTitle"));

		layoutSettingsEditor = new RegistrationFormLayoutSettingsEditor(msg, serverConfig, fileStorageService,
				imageAccessService);

		VerticalLayout wrapper = new VerticalLayout(main, layoutSettingsEditor);
		wrapper.setPadding(false);
		tabs.add(msg.getMessage("RegistrationFormViewer.displayTab"), wrapper);
	}

	private void initLayoutTab()
	{
		VerticalLayout wrapper = new VerticalLayout();
		layoutEditor = new EnquiryFormLayoutEditorTab(msg, this::getEnquiryForm);
		wrapper.setPadding(true);
		wrapper.add(layoutEditor);
		tabs.addSelectedChangeListener(event -> layoutEditor.updateFromForm());
		tabs.add(msg.getMessage("RegistrationFormViewer.layoutTab"), wrapper);
	}

	private void initWrapUpTab()
	{
		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		Span hint = new Span(msg.getMessage("RegistrationFormEditor.onlyForStandaloneEnquiry"));
		hint.addClassName(CssClassNames.ITALIC.getName());
		main.add(hint);
		Component wrapUpComponent = getWrapUpComponent(RegistrationWrapUpConfig.TriggeringState::isSuitableForEnquiry);
		main.add(wrapUpComponent);
		tabs.add(msg.getMessage("RegistrationFormEditor.wrapUpTab"), main);
	}

	private void initAssignedTab()
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setPadding(false);
		tabs.add(msg.getMessage("RegistrationFormViewer.assignedTab"), wrapper);

		profileEditor = new RegistrationTranslationProfileEditor(msg, actionsRegistry, actionComponentProvider,
				notificationPresenter, new TooltipFactory());
		profileEditor.setValue(new TranslationProfile(FORM_PROFILE, "", ProfileType.REGISTRATION,
				new ArrayList<>()));
		wrapper.add(profileEditor);
	}

	private EnquiryForm getEnquiryForm()
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
