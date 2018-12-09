/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.UserError;
import com.vaadin.shared.ui.datefield.DateTimeResolution;
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
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateValidator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.authn.ExpectedIdentity.IdentityExpectation;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.Editor;
import pl.edu.icm.unity.webui.common.NotNullComboBox;
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
	private Map<String, MessageTemplate> msgTemplates;
	private Map<String, RegistrationForm> formsByName;
	private Map<String, AttributeType> attrTypes;

	private NotNullComboBox<String> forms;
	private DateTimeField expiration;
	private TextField contactAddress;
	private EnumComboBox<RemoteIdentityExpectation> remoteIdentityExpectation;
	private List<TextField> messageParams;

	private TabSheet tabs;
	private ListOfEmbeddedElements<PrefilledEntry<IdentityParam>> presetIdentities;
	private ListOfEmbeddedElements<GroupSelectionPair> presetGroups;
	private ListOfEmbeddedElements<PrefilledEntry<Attribute>> presetAttributes;
	private List<Group> allGroups;
	private FormLayout top;
	private MessageTemplateManagement msgTemplateMan;

	public InvitationEditor(UnityMessageSource msg, IdentityEditorRegistry identityEditorRegistry,
			AttributeHandlerRegistry attrHandlersRegistry, Map<String, MessageTemplate> msgTemplates,
			Collection<RegistrationForm> availableForms, Map<String, AttributeType> attrTypes,
			List<Group> allGroups, MessageTemplateManagement msgTemplateMan) throws WrongArgumentException
	{
		this.msg = msg;
		this.identityEditorRegistry = identityEditorRegistry;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.attrTypes = attrTypes;
		this.msgTemplates = msgTemplates;
		this.allGroups = allGroups;
		this.msgTemplateMan = msgTemplateMan;
		initUI(availableForms);
	}

	private void initUI(Collection<RegistrationForm> availableForms) throws WrongArgumentException
	{
		messageParams = new ArrayList<>();

		formsByName = availableForms.stream()
				.filter(form -> form.getRegistrationCode() == null && form.isPubliclyAvailable())
				.collect(Collectors.toMap(RegistrationForm::getName, form -> form));
		if (formsByName.keySet().isEmpty())
			throw new WrongArgumentException(
					"There are no public registration forms to create an invitation for.");

		expiration = new DateTimeField(msg.getMessage("InvitationViewer.expiration"));
		expiration.setRequiredIndicatorVisible(true);
		expiration.setResolution(DateTimeResolution.MINUTE);
		expiration.setValue(LocalDateTime.now(DEFAULT_ZONE_ID).plusDays(DEFAULT_TTL_DAYS));

		contactAddress = new TextField(msg.getMessage("InvitationViewer.contactAddress"));

		remoteIdentityExpectation = new EnumComboBox<>(msg.getMessage("InvitationEditor.requireSameEmail"), msg,
				"InvitationEditor.idExpectation.", RemoteIdentityExpectation.class,
				RemoteIdentityExpectation.NONE);

		Label prefillInfo = new Label(msg.getMessage("InvitationEditor.prefillInfo"));

		tabs = new TabSheet();

		Label channel = new Label();
		channel.setCaption(msg.getMessage("InvitationViewer.channelId"));

		forms = new NotNullComboBox<>(msg.getMessage("InvitationViewer.formId"));
		forms.addValueChangeListener(event -> {
			RegistrationForm registrationForm = formsByName.get(forms.getValue());
			setPerFormUI(registrationForm);

			String invTemplate = registrationForm.getNotificationsConfiguration().getInvitationTemplate();
			if (invTemplate != null && msgTemplates.get(invTemplate) != null)
				channel.setValue(msgTemplates.get(invTemplate).getNotificationChannel());
			else
				channel.setValue("");
		});
		top = new FormLayout();
		top.addComponents(forms, channel, expiration, contactAddress, remoteIdentityExpectation);

		forms.setItems(formsByName.keySet());

		VerticalLayout main = new VerticalLayout(top, prefillInfo, tabs);
		main.setSpacing(true);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	private void setPerFormUI(RegistrationForm form)
	{
		for (Component mparam : messageParams)
			top.removeComponent(mparam);
		tabs.removeAllComponents();

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

	private List<TextField> getMessageParams(RegistrationForm form)
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

	public InvitationParam getInvitation() throws FormValidationException
	{
		String addr = contactAddress.isEmpty() ? null : contactAddress.getValue();
		if (expiration.getValue() == null)
		{
			expiration.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			throw new FormValidationException();
		}
			
		InvitationParam ret = new InvitationParam(forms.getValue(),
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

		if (addr != null && remoteIdentityExpectation.getValue() != RemoteIdentityExpectation.NONE)
		{
			IdentityExpectation expectation = remoteIdentityExpectation
					.getValue() == RemoteIdentityExpectation.HINT ? IdentityExpectation.HINT
							: IdentityExpectation.MANDATORY;
			ret.setExpectedIdentity(new ExpectedIdentity(addr, expectation));
		}
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
		}

		@Override
		public void setEditedComponentPosition(int position)
		{
			memberEditor.setEditedComponentPosition(position);
			GroupRegistrationParam groupRegistrationParam = formParams.get(position);
			List<Group> items = GroupPatternMatcher.filterMatching(allGroups,
					groupRegistrationParam.getGroupPath());
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
