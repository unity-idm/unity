/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.UserError;
import com.vaadin.shared.ui.datefield.DateTimeResolution;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipInfo;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateValidator;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.authn.ExpectedIdentity.IdentityExpectation;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.Editor;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.groups.GroupsSelection;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Edit UI for {@link InvitationParam}.
 * 
 * @author Krzysztof Benedyczak
 */
public class InvitationEditor extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, InvitationEditor.class);

	private enum RemoteIdentityExpectation
	{
		NONE, HINT, REQUIRED
	}

	private static final long DEFAULT_TTL_DAYS = 3;
	private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
	private UnityMessageSource msg;
	private IdentityEditorRegistry identityEditorRegistry;
	private AttributeHandlerRegistry attrHandlersRegistry;
	private NotificationProducer notificationProducer;
	
	private Map<String, MessageTemplate> msgTemplates;
	private Map<String, BaseForm> formsByName;
	private Map<String, AttributeType> attrTypes;
	
	private ComboBox<InvitationType> type;
	
	private ComboBox<Long> entity;
	private ComboBox<String> forms;
	private DateTimeField expiration;
	private TextField contactAddress;
	private EnumComboBox<RemoteIdentityExpectation> remoteIdentityExpectation;
	private List<TextField> messageParams;
	private Label channel;

	private TabSheet tabs;
	private ListOfEmbeddedElements<PrefilledEntry<IdentityParam>> presetIdentities;
	private ListOfEmbeddedElements<GroupSelectionPair> presetGroups;
	private ListOfEmbeddedElements<PrefilledEntry<Attribute>> presetAttributes;
	private List<Group> allGroups;
	private FormLayout top;
	private MessageTemplateManagement msgTemplateMan;
	
	private Collection<RegistrationForm> availableRegistrationForms;
	private Collection<EnquiryForm> availableEnquiryForms;
	private Map<Long, GroupMembershipInfo> allEntities;
	private Map<Long, String> availableEntities;
	private String entityNameAttr;
	
	public InvitationEditor(UnityMessageSource msg, IdentityEditorRegistry identityEditorRegistry,
			AttributeHandlerRegistry attrHandlersRegistry, Map<String, MessageTemplate> msgTemplates,
			Collection<RegistrationForm> availableRegistrationForms,
			Collection<EnquiryForm> availableEnquiryForms, Map<String, AttributeType> attrTypes,
			NotificationProducer notificationsProducer, Map<Long, GroupMembershipInfo> allEntities, String entityNameAttr,
			List<Group> allGroups, MessageTemplateManagement msgTemplateMan) throws WrongArgumentException
	{
		this.msg = msg;
		this.identityEditorRegistry = identityEditorRegistry;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.attrTypes = attrTypes;
		this.msgTemplates = msgTemplates;
		this.allGroups = allGroups;
		this.msgTemplateMan = msgTemplateMan;
		this.availableRegistrationForms = availableRegistrationForms;
		this.availableEnquiryForms = availableEnquiryForms;
		this.availableEntities = new HashMap<>();
		this.allEntities = allEntities;
		this.notificationProducer = notificationsProducer;
		this.entityNameAttr = entityNameAttr;
		initUI();
	}

	private void initUI() throws WrongArgumentException
	{
		messageParams = new ArrayList<>();

		entity = new ComboBox<>(msg.getMessage("InvitationEditor.entity"));
		entity.setEmptySelectionAllowed(false);
		entity.setItemCaptionGenerator(i -> availableEntities.get(i) + " [" + i + "]");
		entity.setWidth(20, Unit.EM);
		entity.addSelectionListener(e -> reloadContactAddress());
		
		type = new ComboBox<>(msg.getMessage("InvitationEditor.type"));
		type.setItemCaptionGenerator(i -> msg.getMessage("InvitationType." + i.toString().toLowerCase()));
		type.setItems(InvitationType.values());
		type.setEmptySelectionAllowed(false);
		type.addValueChangeListener(e -> {
			
			boolean isEnquiry = e.getValue().equals(InvitationType.ENQUIRY);
			remoteIdentityExpectation.setVisible(!isEnquiry);
			entity.setVisible(isEnquiry);
			contactAddress.clear();		
			reloadForms();
		});
		
		expiration = new DateTimeField(msg.getMessage("InvitationViewer.expiration"));
		expiration.setRequiredIndicatorVisible(true);
		expiration.setResolution(DateTimeResolution.MINUTE);
		expiration.setValue(LocalDateTime.now(DEFAULT_ZONE_ID).plusDays(DEFAULT_TTL_DAYS));

		contactAddress = new TextField(msg.getMessage("InvitationViewer.contactAddress"));
		contactAddress.setWidth(20, Unit.EM);

		remoteIdentityExpectation = new EnumComboBox<>(msg.getMessage("InvitationEditor.requireSameEmail"), msg,
				"InvitationEditor.idExpectation.", RemoteIdentityExpectation.class,
				RemoteIdentityExpectation.NONE);

		Label prefillInfo = new Label(msg.getMessage("InvitationEditor.prefillInfo"));

		tabs = new TabSheet();

		channel = new Label();
		channel.setCaption(msg.getMessage("InvitationViewer.channelId"));

		forms = new ComboBox<>(msg.getMessage("InvitationEditor.RegistrationFormId"));
		forms.setRequiredIndicatorVisible(true);
		forms.addValueChangeListener(event -> {
			BaseForm form = formsByName.get(forms.getValue());
			setPerFormUI(form);
			channel.setValue("");
			if (form == null)
			{
				return;
			}
		
			String invTemplate = form.getNotificationsConfiguration().getInvitationTemplate();
			if (invTemplate != null && msgTemplates.get(invTemplate) != null)
				channel.setValue(msgTemplates.get(invTemplate).getNotificationChannel());
			else
				channel.setValue("");
			
			if (type.getValue().equals(InvitationType.ENQUIRY))
			{
				reloadEntities();
			}
		});
		forms.setEmptySelectionAllowed(false);
		
		top = new FormLayout();
		top.addComponents(type, forms, channel, expiration, entity, contactAddress, remoteIdentityExpectation);

		type.setSelectedItem(InvitationType.REGISTRATION);

		VerticalLayout main = new VerticalLayout(top, prefillInfo, tabs);
		main.setSpacing(true);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	private void reloadContactAddress()
	{
		Long entityVal = entity.getValue();
		contactAddress.clear();
		if (entityVal == null)
			return;
		BaseForm form = formsByName.get(forms.getValue());

		if (form == null)
		{
			return;
		}

		String invTemplate = form.getNotificationsConfiguration().getInvitationTemplate();
		if (invTemplate == null)
			return;
		
		try
		{
			contactAddress.setValue(notificationProducer.getAddressForEntity(new EntityParam(entityVal),
					invTemplate, false));
		} catch (EngineException e1)
		{
			log.error("Can not get address for entity " + entityVal);
		}
	}

	
	private void reloadForms()
	{
		if (type.getValue().equals(InvitationType.REGISTRATION))
		{
			formsByName = availableRegistrationForms.stream().filter(
					form -> form.getRegistrationCode() == null && form.isPubliclyAvailable())
					.collect(Collectors.toMap(RegistrationForm::getName, form -> form));
			forms.setCaption(msg.getMessage("InvitationEditor.RegistrationFormId"));
		} else
		{
			formsByName = availableEnquiryForms.stream()
					.collect(Collectors.toMap(EnquiryForm::getName, form -> form));
			forms.setCaption(msg.getMessage("InvitationEditor.EnquiryFormId"));

		}
		
		forms.setItems(formsByName.keySet());
		if (!formsByName.keySet().isEmpty())
		{
			forms.setSelectedItem(formsByName.keySet().iterator().next());
		}else
		{
			forms.setSelectedItem(null);
		}
	}
	
	private void reloadEntities()
	{
		availableEntities.clear();
		EnquiryForm form = availableEnquiryForms.stream().filter(f -> f.getName().equals(forms.getValue()))
				.findFirst().orElse(null);
		if (form == null)
		{
			entity.setItems(Collections.emptyList());
			return;
		}
		
		allEntities.entrySet().stream().filter(e -> e.getValue().relevantEnquiryForm.contains(form.getName()))
				.forEach(e -> availableEntities.put(e.getKey(), getLabel(e.getValue())));

		List<Long> sortedEntities = availableEntities.keySet().stream().sorted().collect(Collectors.toList());
		entity.setItems(sortedEntities);
		entity.setSelectedItem(null);
		if (!sortedEntities.isEmpty())
		{
			entity.setSelectedItem(sortedEntities.iterator().next());
		}
	}

	String getLabel(GroupMembershipInfo info)
	{
		if (entityNameAttr != null && info.attributes.containsKey("/"))
		{
			AttributeExt name = info.attributes.get("/").get(entityNameAttr);
			if (name != null && !name.getValues().isEmpty())
			{
				return name.getValues().get(0);
			}
		}

		return "";
	}
	
	private void setPerFormUI(BaseForm form)
	{
		for (Component mparam : messageParams)
			top.removeComponent(mparam);
		tabs.removeAllComponents();
		if (form == null)
			return;
		
		int idParamsNum = form.getIdentityParams() == null ? 0 : form.getIdentityParams().size();
		presetIdentities = new ListOfEmbeddedElements<>(msg, () -> {
			return new PresetIdentityEditor(identityEditorRegistry, form.getIdentityParams(), msg);
		}, idParamsNum, idParamsNum, true);
		presetIdentities.setCaption(msg.getMessage("InvitationEditor.identities"));
		if (idParamsNum > 0)
			addTabWithMargins(presetIdentities);

		int attrParamsNum = form.getAttributeParams() == null ? 0 : form.getAttributeParams().size();
		presetAttributes = new ListOfEmbeddedElements<>(msg, () -> {
			return new PresetAttributeEditor(msg, form.getAttributeParams(), attrHandlersRegistry,
					attrTypes);
		}, attrParamsNum, attrParamsNum, true);
		presetAttributes.setCaption(msg.getMessage("InvitationEditor.attributes"));
		if (attrParamsNum > 0)
			addTabWithMargins(presetAttributes);

		int groupParamsNum = form.getGroupParams() == null ? 0 : form.getGroupParams().size();
		presetGroups = new ListOfEmbeddedElements<>(msg, () -> {
			return new PresetMembershipEditorWithAllowedGroups(msg, allGroups, form.getGroupParams());
		}, groupParamsNum, groupParamsNum, true);
		presetGroups.setCaption(msg.getMessage("InvitationEditor.groups"));
		if (groupParamsNum > 0)
			addTabWithMargins(presetGroups);

		messageParams = getMessageParams(form);
		for (Component mparam : messageParams)
			top.addComponent(mparam);
	}

	private List<TextField> getMessageParams(BaseForm form)
	{
		String invitationTemplate = form.getNotificationsConfiguration().getInvitationTemplate();
		if (invitationTemplate == null)
			return Collections.emptyList();
		MessageTemplate msgTemplate;
		try
		{
			msgTemplate = msgTemplateMan.getTemplate(invitationTemplate);
		} catch (EngineException e)
		{
			log.error("Can not read invitation template of the form, won't fill any parameters", e);
			return Collections.emptyList();
		}

		Set<String> variablesSet = MessageTemplateValidator.extractCustomVariables(msgTemplate.getMessage());
		List<String> variables = new ArrayList<>(variablesSet);
		Collections.sort(variables);

		List<TextField> ret = new ArrayList<>();
		for (String variable : variables)
		{
			String caption = variable.startsWith(MessageTemplateDefinition.CUSTOM_VAR_PREFIX)
					? variable.substring(MessageTemplateDefinition.CUSTOM_VAR_PREFIX.length())
					: variable;
			if (!caption.isEmpty())
				caption = Character.toUpperCase(caption.charAt(0)) + caption.substring(1);
			TextField field = new TextField(caption + ":");
			field.setData(variable);
			ret.add(field);
		}
		return ret;
	}

	private void addTabWithMargins(Component src)
	{
		VerticalLayout wrapper = new VerticalLayout(src);
		wrapper.setMargin(true);
		wrapper.setSpacing(false);
		tabs.addTab(wrapper).setCaption(src.getCaption());
		src.setCaption("");
	}

	private InvitationParam getInvitationParam(String form, Instant expiration, String addr) throws FormValidationException
	{
		if (type.getValue().equals(InvitationType.REGISTRATION))
		{
			return getRegistrationInvitationParam(form, expiration, addr);

		} else
		{
			return getEnquiryInvitationParam(form, expiration, addr);
		}
	}

	private EnquiryInvitationParam getEnquiryInvitationParam(String form, Instant expiration, String addr) throws FormValidationException
	{
		EnquiryInvitationParam param = new EnquiryInvitationParam(form, expiration, addr);
		
		if (entity.getValue() == null)
		{
			entity.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			throw new FormValidationException();
		}
		
		param.setEntity(entity.getValue());
		return param;
	}

	private RegistrationInvitationParam getRegistrationInvitationParam(String form, Instant expiration, String addr)
	{
		RegistrationInvitationParam param = new RegistrationInvitationParam(form, expiration, addr);
		if (addr != null && remoteIdentityExpectation.getValue() != RemoteIdentityExpectation.NONE)
		{
			IdentityExpectation expectation = remoteIdentityExpectation
					.getValue() == RemoteIdentityExpectation.HINT ? IdentityExpectation.HINT
							: IdentityExpectation.MANDATORY;
			param.setExpectedIdentity(new ExpectedIdentity(addr, expectation));
		}
		return param;
	}
	
	public InvitationParam getInvitation() throws FormValidationException
	{
		if (forms.getValue() == null)
		{
			forms.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			throw new FormValidationException();
		
		}
		String addr = contactAddress.isEmpty() ? null : contactAddress.getValue();
		if (expiration.getValue() == null)
		{
			expiration.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			throw new FormValidationException();
		}
			
		InvitationParam ret = getInvitationParam(forms.getValue(),
				expiration.getValue().atZone(DEFAULT_ZONE_ID).toInstant(), addr);

		prefill(presetIdentities.getElements(), ret.getIdentities());
		prefill(presetAttributes.getElements(), ret.getAttributes());
		prefill(presetGroups.getElements().stream()
				.map(v -> v.groupSelection).collect(Collectors.toList()), ret.getGroupSelections());
		prefill(presetGroups.getElements().stream().map(v -> v.allowedGroupSelection)
				.collect(Collectors.toList()), ret.getAllowedGroups());
		Map<String, String> customParams = messageParams.stream().collect(Collectors.toMap(
				paramField -> (String) paramField.getData(), paramField -> paramField.getValue()));
		ret.getMessageParams().putAll(customParams);

		
		return ret;
	}

	private <T> void prefill(List<T> input, Map<Integer, T> output)
	{
		for (int i = 0; i < input.size(); i++)
		{
			T element = input.get(i);
			if (element != null)
				output.put(i, element);
		}
	}

	private static class PresetMembershipEditorWithAllowedGroups implements Editor<GroupSelectionPair>
	{

		private PresetMembershipEditor memberEditor;
		private GroupsSelection allowedGroupSelection;
		private List<Group> allGroups;
		private List<GroupRegistrationParam> formParams;

		public PresetMembershipEditorWithAllowedGroups(UnityMessageSource msg, List<Group> allGroups,
				List<GroupRegistrationParam> formParams)
		{
			this.formParams = formParams;
			this.allGroups = allGroups;
			memberEditor = new PresetMembershipEditor(msg, allGroups, formParams);
			allowedGroupSelection = GroupsSelection.getGroupsSelection(msg, true, false);
			allowedGroupSelection.setCaption(msg.getMessage("InvitationEditor.limitTo"));
			allowedGroupSelection.setDescription(msg.getMessage("InvitationEditor.limitToDescription"));
		}

		@Override
		public void setEditedComponentPosition(int position)
		{
			memberEditor.setEditedComponentPosition(position);
			GroupRegistrationParam groupRegistrationParam = formParams.get(position);
			List<Group> items = GroupPatternMatcher.filterByIncludeGroupsMode(
					GroupPatternMatcher.filterMatching(allGroups,
							groupRegistrationParam.getGroupPath()),
					groupRegistrationParam.getIncludeGroupsMode());
			allowedGroupSelection.setItems(items);
		}

		@Override
		public ComponentsContainer getEditorComponent(GroupSelectionPair value, int position)
		{
			ComponentsContainer container = new ComponentsContainer();
			container.add(allowedGroupSelection);
			container.add(memberEditor
					.getEditorComponent(value != null ? value.groupSelection : null, position)
					.getComponents());
			return container;
		}

		@Override
		public GroupSelectionPair getValue() throws FormValidationException
		{
			return new GroupSelectionPair(memberEditor.getValue(),
					new GroupSelection(allowedGroupSelection.getSelectedGroups()));
		}
	}
	
	private static class GroupSelectionPair
	{
		public final PrefilledEntry<GroupSelection> groupSelection;
		public final GroupSelection allowedGroupSelection;
		
		public GroupSelectionPair(PrefilledEntry<GroupSelection> groupSelection, GroupSelection allowedGroupSelection)
		{
			this.groupSelection = groupSelection;
			this.allowedGroupSelection = allowedGroupSelection;
		}
	}
}
