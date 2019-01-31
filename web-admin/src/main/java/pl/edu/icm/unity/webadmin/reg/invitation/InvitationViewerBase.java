/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webadmin.reg.invitation;

import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

public abstract class InvitationViewerBase extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, InvitationViewerBase.class);
	
	private AttributeHandlerRegistry attrHandlersRegistry;
	private MessageTemplateManagement msgTemplateMan;
	protected UnityMessageSource msg;
	protected SharedEndpointManagement sharedEndpointMan;
	private GroupsManagement groupMan;
	
	private Label type;
	private Label formId;
	private Label code;
	private Label expiration;
	private Label contactAddress;
	private Label channelId;
	private Label lastSentTime;
	private Label notificationsSent;
	private Label messageParams;
	private Link link;
	private ListOfElementsWithActions<PrefilledEntry<IdentityParam>> identities;
	private VerticalLayout attributes;
	private VerticalLayout groups;

	private SafePanel identitiesPanel;
	private SafePanel attributesPanel;
	private SafePanel groupsPanel;
	private FormLayout main;
	
	protected BaseForm form;
	
	public InvitationViewerBase(AttributeHandlerRegistry attrHandlersRegistry,
			MessageTemplateManagement msgTemplateMan, UnityMessageSource msg, SharedEndpointManagement sharedEndpointMan, GroupsManagement groupMan)
	{
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.msgTemplateMan = msgTemplateMan;
		this.msg = msg;
		this.sharedEndpointMan = sharedEndpointMan;
		this.groupMan = groupMan;
		initUI();
	}
	
	
	private void initUI()
	{
		main = new CompactFormLayout();
		setCompositionRoot(main);

		type = new Label();
		type.setCaption(msg.getMessage("InvitationViewer.type"));
		
		formId = new Label();

		code = new Label();
		code.setCaption(msg.getMessage("InvitationViewer.code"));

		link = new Link();
		link.setCaption(msg.getMessage("InvitationViewer.link"));

		expiration = new Label();
		expiration.setCaption(msg.getMessage("InvitationViewer.expiration"));

		channelId = new Label();
		channelId.setCaption(msg.getMessage("InvitationViewer.channelId"));

		contactAddress = new Label();
		contactAddress.setCaption(msg.getMessage("InvitationViewer.contactAddress"));

		lastSentTime = new Label();
		lastSentTime.setCaption(msg.getMessage("InvitationViewer.lastSentTime"));

		notificationsSent = new Label();
		notificationsSent.setCaption(msg.getMessage("InvitationViewer.notificationsSent"));

		messageParams = new Label();
		messageParams.setWidth(100, Unit.PERCENTAGE);
		messageParams.setCaption(msg.getMessage("InvitationViewer.messageParams"));

		identities = new ListOfElementsWithActions<>();
		identitiesPanel = new SafePanel(msg.getMessage("InvitationViewer.identities"), identities);

		attributes = new VerticalLayout();
		attributes.setSpacing(true);
		attributes.addStyleName(Styles.tinySpacing.toString());

		attributes.setMargin(true);
		attributesPanel = new SafePanel(msg.getMessage("InvitationViewer.attributes"), attributes);

		groups = new VerticalLayout();
		groups.setSpacing(true);
		groups.addStyleName(Styles.tinySpacing.toString());

		groups.setMargin(true);

		groupsPanel = new SafePanel(msg.getMessage("InvitationViewer.groups"), groups);
		groupsPanel.setWidth(100, Unit.PERCENTAGE);

		main.addComponents(type, formId, code, link, expiration, channelId);
		main.addComponents(getAdditionalFields().getComponents());
		main.addComponents(contactAddress, lastSentTime, notificationsSent, messageParams, identitiesPanel,
				attributesPanel, groupsPanel);
		setInput(null);
	}
	
	protected abstract ComponentsContainer getAdditionalFields();
	
	protected abstract BaseForm getForm(String id);
	
	public boolean setInput(InvitationWithCode invitationWithCode)
	{
		if (invitationWithCode == null)
		{
			setVisible(false);
			return false;
		} else
		{
			setVisible(true);
		}
		
		InvitationParam invitation = invitationWithCode.getInvitation();
		InvitationType itype = invitation.getType();
		
		form = getForm(invitation.getFormId());
		if (form == null)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("InvitationViewer.errorMissingForm"));
			setCompositionRoot(error);
			return false;
		} else
		{
			setCompositionRoot(main);
		}
		

		type.setValue(msg.getMessage("InvitationType." + itype.toString().toLowerCase()));
		
		formId.setValue(invitation.getFormId());
		code.setValue(invitationWithCode.getRegistrationCode());
		expiration.setValue(TimeUtil.formatMediumInstant(invitation.getExpiration()));
		contactAddress.setValue(invitation.getContactAddress());
		channelId.setValue(getChannel(form));
		notificationsSent.setValue(String.valueOf(invitationWithCode.getNumberOfSends()));
		lastSentTime.setValue(invitationWithCode.getLastSentTime() != null
				? TimeUtil.formatMediumInstant(invitationWithCode.getLastSentTime())
				: "-");
		messageParams.setVisible(!invitation.getMessageParams().isEmpty());
		messageParams.setValue(invitation.getMessageParams().toString());

		identitiesPanel.setVisible(!invitation.getIdentities().isEmpty());
		identities.clearContents();
		invitation.getIdentities().values().forEach(e -> identities.addEntry(e));

		attributesPanel.setVisible(!invitation.getAttributes().isEmpty());
		attributes.removeAllComponents();
		invitation.getAttributes().values().forEach(entry -> {
			String attr = attrHandlersRegistry.getSimplifiedAttributeRepresentation(entry.getEntry());
			Label l = new Label("[" + entry.getMode().name() + "] " + attr);
			attributes.addComponent(l);
		});

		groupsPanel.setVisible(
				!invitation.getGroupSelections().isEmpty() || !invitation.getAllowedGroups().isEmpty());
		groups.removeAllComponents();
		setGroups(form, invitation);
		return true;
	}
	
	private void setGroups(BaseForm form, InvitationParam invitation)
	{
		for (int i = 0; i < form.getGroupParams().size(); i++)
		{
			GroupRegistrationParam gp = form.getGroupParams().get(i);
			if (gp == null)
				continue;

			StringBuilder row = new StringBuilder("[" + gp.getGroupPath() + "]");
			PrefilledEntry<GroupSelection> selectedGroup = invitation.getGroupSelections().get(i);
			boolean add = false;
			if (selectedGroup != null && !selectedGroup.getEntry().getSelectedGroups().isEmpty())
			{
				row.append(" [" + selectedGroup.getMode().name() + "] ");
				row.append(selectedGroup.getEntry().getSelectedGroups().stream()
						.map(g -> getGroupDisplayedName(g)).collect(Collectors.toList()));
				add = true;
			}

			GroupSelection allowed = invitation.getAllowedGroups().get(i);
			if (allowed != null && !allowed.getSelectedGroups().isEmpty())
			{
				row.append(" " + msg.getMessage("InvitationViewer.groupLimitedTo") + " ");
				row.append(allowed.getSelectedGroups().stream().map(g -> getGroupDisplayedName(g))
						.collect(Collectors.toList()));
				add = true;
			}

			if (add)
			{
				groups.addComponent(new Label(row.toString()));
			}
		}
	}

	protected void setLink(String linkURL)
	{
		link.setTargetName("_blank");
		link.setResource(new ExternalResource(linkURL));
	}
	
	protected void setFormCaption(String caption)
	{
		formId.setCaption(caption);
	}
	
	private String getChannel(BaseForm form)
	{
		try
		{
			return msgTemplateMan.getTemplate(form.getNotificationsConfiguration().getInvitationTemplate())
					.getNotificationChannel();

		} catch (Exception e)
		{
			return "";
		}
	}
	
	protected String getGroupDisplayedName(String path)
	{
		try
		{
			return groupMan.getContents(path, GroupContents.METADATA).getGroup().getDisplayedName().getValue(msg);
		} catch (Exception e)
		{
			log.warn("Can not get group displayed name for group " + path);
			return path;
		}
	}
}
