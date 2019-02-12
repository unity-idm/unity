/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.vaadin.data.Binder;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentProvider;
import pl.edu.icm.unity.webadmin.tprofile.RegistrationTranslationProfileEditor;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupsSelectionList;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.mvel.MVELExpressionField;

/**
 * Allows to edit an {@link EnquiryForm}. Can be configured to edit an existing form (name is fixed)
 * or to create a new one (name can be chosen).
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class EnquiryFormEditor extends BaseFormEditor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryFormEditor.class);
	
	private UnityMessageSource msg;
	private GroupsManagement groupsMan;
	private NotificationsManagement notificationsMan;
	private MessageTemplateManagement msgTempMan;
	
	private TabSheet tabs;
	private CheckBox ignoreRequestsAndInvitation;
	
	private EnumComboBox<EnquiryType> enquiryType;
	private GroupsSelectionList targetGroups;
	private MVELExpressionField targetCondition;
	private EnquiryFormNotificationsEditor notificationsEditor;
	private RegistrationFormLayoutSettingsEditor layoutSettingsEditor;
	private CheckBox byInvitationOnly;
	
	private RegistrationActionsRegistry actionsRegistry;
	private ActionParameterComponentProvider actionComponentProvider;
	private RegistrationTranslationProfileEditor profileEditor;
	private EnquiryFormLayoutEditorTab layoutEditor;
	//binder is only for targetCondition validation
	private Binder<EnquiryForm> binder;
	
	
	@Autowired
	public EnquiryFormEditor(UnityMessageSource msg, GroupsManagement groupsMan,
			NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan, IdentityTypeSupport identitiesMan,
			AttributeTypeManagement attributeMan,
			CredentialManagement authenticationMan, RegistrationActionsRegistry actionsRegistry,
			ActionParameterComponentProvider actionComponentFactory)
			throws EngineException
	{
		super(msg, identitiesMan, attributeMan, authenticationMan);
		this.actionsRegistry = actionsRegistry;
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.notificationsMan = notificationsMan;
		this.msgTempMan = msgTempMan;
		actionComponentProvider = actionComponentFactory;
		this.actionComponentProvider.init();
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
		setWidth(100, Unit.PERCENTAGE);
		setHeight(100, Unit.PERCENTAGE);
		setSpacing(true);
		tabs = new TabSheet();
		initMainTab();
		initCollectedTab();
		initDisplayedTab();
		initLayoutTab();
		initWrapUpTab();
		initAssignedTab();
		ignoreRequestsAndInvitation = new CheckBox(
				msg.getMessage("RegistrationFormEditDialog.ignoreRequestsAndInvitations"));
		addComponent(ignoreRequestsAndInvitation);
		setComponentAlignment(ignoreRequestsAndInvitation, Alignment.TOP_RIGHT);

		addComponent(tabs);
		setComponentAlignment(tabs, Alignment.TOP_LEFT);
		setExpandRatio(tabs, 1);
	}
	
	public EnquiryForm getForm() throws FormValidationException
	{
		EnquiryFormBuilder builder = getFormBuilderBasic();
		
		builder.withTranslationProfile(profileEditor.getProfile());
		EnquiryFormNotifications notCfg = notificationsEditor.getValue();
		builder.withNotificationsConfiguration(notCfg);
		FormLayoutSettings settings = layoutSettingsEditor.getSettings();
		builder.withFormLayoutSettings(settings);
		
		builder.withLayout(layoutEditor.getLayout());
		builder.withByInvitationOnly(byInvitationOnly.getValue());
		
		EnquiryForm form = builder.build();
		try
		{
			form.validateLayout();
		} catch (Exception e)
		{
			throw new FormValidationException(e);
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
		builder.withTargetGroups(targetGroups.getSelectedGroups().toArray(new String[0]));
		builder.withTargetCondition(targetCondition.getValue());
		return builder;
	}
	
	public void setForm(EnquiryForm toEdit)
	{
		super.setValue(toEdit);
		notificationsEditor.setValue(toEdit.getNotificationsConfiguration());
		enquiryType.setValue(toEdit.getType());
		targetGroups.setSelectedGroups(Lists.newArrayList(toEdit.getTargetGroups()));
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
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(false);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.mainTab"));
		
		initNameAndDescFields(msg.getMessage("EnquiryFormEditor.defaultName"));
		main.addComponents(name, description);

		notificationsEditor = new EnquiryFormNotificationsEditor(msg, groupsMan, 
				notificationsMan, msgTempMan);
		enquiryType = new EnumComboBox<>(msg.getMessage("EnquiryFormViewer.type"), 
				msg, "EnquiryType.", EnquiryType.class, 
				EnquiryType.REQUESTED_OPTIONAL);
		enquiryType.setWidth(20, Unit.EM);
		enquiryType.addValueChangeListener(e -> {

			boolean enable = !e.getValue().equals(EnquiryType.STICKY);
			setCredentialsTabVisible(enable);
			setIdentitiesTabVisible(enable);
			if (!enable)
			{
				resetCredentialTab();
				resetIdentitiesTab();
			}

		});
		
		
		targetGroups = new GroupsSelectionList(msg.getMessage("EnquiryFormViewer.targetGroups"), 
				notificationsEditor.getGroups());
		targetGroups.setInput("/", true);
		targetGroups.setRequiredIndicatorVisible(true);
		
		targetCondition = new MVELExpressionField(msg, msg.getMessage("EnquiryFormEditor.targetCondition"),
				msg.getMessage("EnquiryFormEditor.targetConditionDesc"));
		
		targetCondition.configureBinding(binder, "targetCondition", false);
		
		byInvitationOnly = new CheckBox(msg.getMessage("RegistrationFormEditor.byInvitationOnly"));
		
		main.addComponents(enquiryType, byInvitationOnly, targetGroups, targetCondition);
		
		notificationsEditor.addToLayout(main);
	}
	
	private void initCollectedTab()
	{
		FormLayout main = new CompactFormLayout();
		collectComments = new CheckBox(msg.getMessage("RegistrationFormEditor.collectComments"));
		main.addComponents(collectComments);
		
		TabSheet tabOfLists = createCollectedParamsTabs(notificationsEditor.getGroups(), true);
		
		VerticalLayout wrapper = new VerticalLayout(main, tabOfLists);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.collectedTab"));
	}
	
	private void initDisplayedTab()
	{
		FormLayout main = new CompactFormLayout();
		initCommonDisplayedFields();
		main.addComponents(displayedName, formInformation, pageTitle);
		
		layoutSettingsEditor = new RegistrationFormLayoutSettingsEditor(msg);
		
		VerticalLayout wrapper = new VerticalLayout(main, layoutSettingsEditor);
		wrapper.setMargin(true);
		wrapper.setSpacing(false);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.displayTab"));
	}
	
	private void initLayoutTab()
	{
		VerticalLayout wrapper = new VerticalLayout();
		layoutEditor = new EnquiryFormLayoutEditorTab(msg, this::getEnquiryForm);
		wrapper.setMargin(true);
		wrapper.setSpacing(false);
		wrapper.addComponent(layoutEditor);
		tabs.addSelectedTabChangeListener(event -> layoutEditor.updateFromForm());
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.layoutTab"));
	}
	
	private void initWrapUpTab() throws EngineException
	{
		VerticalLayout main = new VerticalLayout();
		Label hint = new Label(msg.getMessage("RegistrationFormEditor.onlyForStandaloneEnquiry"));
		hint.addStyleName(Styles.emphasized.toString());
		hint.setWidth(100, Unit.PERCENTAGE);
		main.addComponent(hint);
		Component wrapUpComponent = getWrapUpComponent(t -> t.isSuitableForEnquiry());
		main.addComponent(wrapUpComponent);
		tabs.addTab(main, msg.getMessage("RegistrationFormEditor.wrapUpTab"));
	}
	
	private void initAssignedTab() throws EngineException
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(true);
		wrapper.setSpacing(false);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.assignedTab"));
		
		profileEditor = new RegistrationTranslationProfileEditor(msg, actionsRegistry, actionComponentProvider);
		profileEditor.setValue(new TranslationProfile("form profile", "", ProfileType.REGISTRATION,
				new ArrayList<>()));
		wrapper.addComponent(profileEditor);
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
}
