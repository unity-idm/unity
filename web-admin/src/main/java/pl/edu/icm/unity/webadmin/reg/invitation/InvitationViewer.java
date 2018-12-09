/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Presents an {@link InvitationWithCode}
 * 
 * @author Krzysztof Benedyczak
 */
public class InvitationViewer extends CustomComponent
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry attrHandlersRegistry;
	private MessageTemplateManagement msgTemplateMan;
	private RegistrationsManagement regMan;
	private SharedEndpointManagement sharedEndpointMan;

	private Label formId;
	private Label code;
	private Label expiration;
	private Label contactAddress;
	private Label channelId;
	private Label lastSentTime;
	private Label notificationsSent;
	private Label messageParams;
	private Label expectedIdentity;
	private Link link;
	private ListOfElementsWithActions<PrefilledEntry<IdentityParam>> identities;
	private VerticalLayout attributes;
	private VerticalLayout groups;

	private SafePanel identitiesPanel;
	private SafePanel attributesPanel;
	private SafePanel groupsPanel;
	private FormLayout main;

	public InvitationViewer(UnityMessageSource msg, AttributeHandlerRegistry attrHandlersRegistry,
			MessageTemplateManagement msgTemplateMan, RegistrationsManagement regMan,
			SharedEndpointManagement sharedEndpointMan)
	{
		this.msg = msg;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.msgTemplateMan = msgTemplateMan;
		this.regMan = regMan;
		this.sharedEndpointMan = sharedEndpointMan;
		initUI();
	}

	private void initUI()
	{
		main = new CompactFormLayout();
		setCompositionRoot(main);

		formId = new Label();
		formId.setCaption(msg.getMessage("InvitationViewer.formId"));

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

		expectedIdentity = new Label();
		expectedIdentity.setWidth(100, Unit.PERCENTAGE);
		expectedIdentity.setCaption(msg.getMessage("InvitationViewer.expectedIdentity"));

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

		main.addComponents(formId, code, link, expiration, channelId, contactAddress, lastSentTime,
				notificationsSent, expectedIdentity, messageParams, identitiesPanel, attributesPanel,
				groupsPanel);
		setInput(null, null);
	}

	public void setInput(InvitationWithCode invitation, RegistrationForm form)
	{
		if (invitation == null)
		{
			setVisible(false);
			return;
		} else
		{
			setVisible(true);
		}
		if (form == null)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("InvitationViewer.errorMissingForm"));
			setCompositionRoot(error);
			return;
		} else
		{
			setCompositionRoot(main);
		}

		formId.setValue(invitation.getFormId());
		code.setValue(invitation.getRegistrationCode());
		expiration.setValue(TimeUtil.formatMediumInstant(invitation.getExpiration()));
		contactAddress.setValue(invitation.getContactAddress());
		channelId.setValue(getChannel(invitation.getFormId()));
		notificationsSent.setValue(String.valueOf(invitation.getNumberOfSends()));
		lastSentTime.setValue(invitation.getLastSentTime() != null
				? TimeUtil.formatMediumInstant(invitation.getLastSentTime())
				: "-");

		String linkURL = PublicRegistrationURLSupport.getPublicRegistrationLink(form,
				invitation.getRegistrationCode(), sharedEndpointMan);
		link.setTargetName("_blank");
		link.setResource(new ExternalResource(linkURL));

		expectedIdentity.setVisible(invitation.getExpectedIdentity() != null);
		if (invitation.getExpectedIdentity() != null)
			expectedIdentity.setValue(invitation.getExpectedIdentity().toString());

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
				row.append(selectedGroup.getEntry().getSelectedGroups());
				add = true;
			}

			GroupSelection allowed = invitation.getAllowedGroups().get(i);
			if (allowed != null && !allowed.getSelectedGroups().isEmpty())
			{
				row.append(" " + msg.getMessage("InvitationViewer.groupLimitedTo") + " ");
				row.append(allowed.getSelectedGroups());
				add = true;
			}

			if (add)
			{
				groups.addComponent(new Label(row.toString()));
			}
		}
	}

	private String getChannel(String formId)
	{
		try
		{
			RegistrationForm form = regMan.getForms().stream().filter(r -> r.getName().equals(formId))
					.findFirst().get();
			return msgTemplateMan.getTemplate(form.getNotificationsConfiguration().getInvitationTemplate())
					.getNotificationChannel();

		} catch (Exception e)
		{
			return "";
		}
	}

}
