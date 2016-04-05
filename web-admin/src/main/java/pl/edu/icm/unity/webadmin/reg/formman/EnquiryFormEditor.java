/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.EnquiryTranslationProfile;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.webadmin.reg.formman.layout.FormLayoutEditor;
import pl.edu.icm.unity.webadmin.reg.formman.layout.FormLayoutEditor.FormProvider;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentFactory.Provider;
import pl.edu.icm.unity.webadmin.tprofile.RegistrationTranslationProfileEditor;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupsSelectionList;

/**
 * Allows to edit an {@link EnquiryForm}. Can be configured to edit an existing form (name is fixed)
 * or to create a new one (name can be chosen).
 * 
 * @author K. Benedyczak
 */
public class EnquiryFormEditor extends BaseFormEditor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryFormEditor.class);
	private UnityMessageSource msg;
	private GroupsManagement groupsMan;
	private NotificationsManagement notificationsMan;
	private MessageTemplateManagement msgTempMan;
	
	private TabSheet tabs;
	private CheckBox ignoreRequests;
	
	private EnumComboBox<EnquiryType> enquiryType;
	private GroupsSelectionList targetGroups;
	private EnquiryFormNotificationsEditor notificationsEditor;
	
	private RegistrationActionsRegistry actionsRegistry;
	private Provider actionComponentProvider;
	private RegistrationTranslationProfileEditor profileEditor;
	private FormLayoutEditor layoutEditor;
	
	public EnquiryFormEditor(UnityMessageSource msg, GroupsManagement groupsMan,
			NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan, IdentitiesManagement identitiesMan,
			AttributesManagement attributeMan,
			AuthenticationManagement authenticationMan, RegistrationActionsRegistry actionsRegistry,
			Provider actionComponentProvider) 
					throws EngineException
	{
		this(msg, groupsMan, notificationsMan, msgTempMan, identitiesMan, attributeMan, authenticationMan, 
				actionsRegistry, actionComponentProvider, false);
	}

	public EnquiryFormEditor(UnityMessageSource msg, GroupsManagement groupsMan,
			NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan, IdentitiesManagement identitiesMan,
			AttributesManagement attributeMan,
			AuthenticationManagement authenticationMan, RegistrationActionsRegistry actionsRegistry,
			Provider actionComponentProvider,
			boolean copyMode)
			throws EngineException
	{
		super(msg, identitiesMan, attributeMan, authenticationMan, copyMode);
		this.actionsRegistry = actionsRegistry;
		this.actionComponentProvider = actionComponentProvider;
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.notificationsMan = notificationsMan;
		this.msgTempMan = msgTempMan;
		initUI();
	}

	private void initUI() throws EngineException
	{
		setWidth(100, Unit.PERCENTAGE);
		setHeight(100, Unit.PERCENTAGE);
		setSpacing(true);
		tabs = new TabSheet();
		initMainTab();
		initCollectedTab();
		initLayoutTab();
		initAssignedTab();
		ignoreRequests = new CheckBox(msg.getMessage("RegistrationFormEditDialog.ignoreRequests"));
		addComponent(ignoreRequests);
		setComponentAlignment(ignoreRequests, Alignment.TOP_RIGHT);
		ignoreRequests.setVisible(false);
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
		builder.withLayout(layoutEditor.getLayout());
		return builder.build();
	}
	
	private EnquiryFormBuilder getFormBuilderBasic() throws FormValidationException
	{
		EnquiryFormBuilder builder = new EnquiryFormBuilder();
		super.buildCommon(builder);
		
		builder.withType(enquiryType.getSelectedValue());
		builder.withTargetGroups(targetGroups.getSelectedGroups().toArray(new String[0]));
		return builder;
	}
	
	public void setForm(EnquiryForm toEdit)
	{
		super.setValue(toEdit);
		notificationsEditor.setValue(toEdit.getNotificationsConfiguration());
		enquiryType.setEnumValue(toEdit.getType());
		targetGroups.setSelectedGroups(Lists.newArrayList(toEdit.getTargetGroups()));
		
		EnquiryTranslationProfile profile = new EnquiryTranslationProfile(
				toEdit.getTranslationProfile().getName(), 
				toEdit.getTranslationProfile().getRules(), actionsRegistry);
		profileEditor.setValue(profile);
		layoutEditor.setInitialForm(toEdit);
		if (!copyMode)
			ignoreRequests.setVisible(true);
	}
	
	private void initMainTab() throws EngineException
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.mainTab"));
		
		initNameAndDescFields(msg.getMessage("EnquiryFormEditor.defaultName"));
		main.addComponents(name, description);

		notificationsEditor = new EnquiryFormNotificationsEditor(msg, groupsMan, 
				notificationsMan, msgTempMan);
		enquiryType = new EnumComboBox<EnquiryForm.EnquiryType>(msg.getMessage("EnquiryFormViewer.type"), 
				msg, "EnquiryType.", EnquiryType.class, 
				EnquiryType.REQUESTED_OPTIONAL);
		
		targetGroups = new GroupsSelectionList(msg.getMessage("EnquiryFormViewer.targetGroups"), 
				notificationsEditor.getGroups());
		targetGroups.setInput("/", true);
		targetGroups.setRequired(true);
		
		main.addComponents(enquiryType, targetGroups);
		
		notificationsEditor.addToLayout(main);
	}
	
	private void initCollectedTab()
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.collectedTab"));
		
		initCommonDisplayedFields();
		
		TabSheet tabOfLists = createCollectedParamsTabs(notificationsEditor.getGroups(), true, 0);
		main.addComponents(displayedName, formInformation, collectComments, tabOfLists);
	}
	
	private void initLayoutTab()
	{
		VerticalLayout wrapper = new VerticalLayout();
		layoutEditor = new FormLayoutEditor(msg, new FormProviderImpl());
		wrapper.setMargin(true);
		wrapper.addComponent(layoutEditor);
		tabs.addSelectedTabChangeListener(event -> layoutEditor.updateFromForm());
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.layoutTab"));
	}
	
	private void initAssignedTab() throws EngineException
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.assignedTab"));
		
		profileEditor = new RegistrationTranslationProfileEditor(msg, actionsRegistry, actionComponentProvider);
		profileEditor.setValue(new EnquiryTranslationProfile("form profile", new ArrayList<>(), 
				actionsRegistry));
		wrapper.addComponent(profileEditor);
	}

	public boolean isIgnoreRequests()
	{
		return ignoreRequests.getValue();
	}
	
	private class FormProviderImpl implements FormProvider
	{
		@Override
		public BaseForm getForm()
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
	}
}
