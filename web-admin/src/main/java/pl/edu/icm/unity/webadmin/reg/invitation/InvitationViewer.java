/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.util.AbstractMap;
import java.util.Map;

import pl.edu.icm.unity.server.utils.TimeUtil;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

/**
 * Presents an {@link InvitationWithCode}
 * @author Krzysztof Benedyczak
 */
public class InvitationViewer extends CustomComponent
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry attrHandlersRegistry;
	
	private Label formId;
	private Label code;
	private Label expiration;
	private Label contactAddress;
	private Label channelId;
	private Label lastSentTime;
	private Label notificationsSent;
	private ListOfElements<PrefilledEntry<IdentityParam>> identities;
	private ListOfElements<PrefilledEntry<Attribute<?>>> attributes;
	private ListOfElements<Map.Entry<String, PrefilledEntry<Selection>>> groups;

	private SafePanel identitiesPanel;
	private SafePanel attributesPanel;
	private SafePanel groupsPanel;
	private FormLayout main;
	
	public InvitationViewer(UnityMessageSource msg,
			AttributeHandlerRegistry attrHandlersRegistry)
	{
		this.msg = msg;
		this.attrHandlersRegistry = attrHandlersRegistry;
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
		
		identities = new ListOfElements<>(msg);
		identities.setMargin(true);
		identitiesPanel = new SafePanel(msg.getMessage("InvitationViewer.identities"), identities);
		
		attributes = new ListOfElements<>(msg, entry -> {
			String attr = attrHandlersRegistry.getSimplifiedAttributeRepresentation(entry.getEntry(), 
					AttributeHandlerRegistry.DEFAULT_MAX_LEN * 2);
			return new Label("[" + entry.getMode().name() + "] " + attr);
		});
		attributes.setMargin(true);
		attributesPanel = new SafePanel(msg.getMessage("InvitationViewer.attributes"), attributes);

		groups = new ListOfElements<>(msg, entry -> {
			String group = entry.getKey();
			PrefilledEntryMode mode = entry.getValue().getMode();
			boolean selected = entry.getValue().getEntry().isSelected();
			String msgKey = selected ? "InvitationViewer.groupSelected" : "InvitationViewer.groupNotSelected";
			return new Label("[" + mode.name() + "] " + msg.getMessage(msgKey, group));
		});
		groups.setMargin(true);
		groupsPanel = new SafePanel(msg.getMessage("InvitationViewer.groups"), groups);

		
		main.addComponents(formId, code, expiration, channelId, contactAddress, lastSentTime, notificationsSent,
				identitiesPanel, attributesPanel, groupsPanel);
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
		channelId.setValue(invitation.getChannelId());
		notificationsSent.setValue(String.valueOf(invitation.getNumberOfSends()));
		lastSentTime.setValue(invitation.getLastSentTime() != null ? 
				TimeUtil.formatMediumInstant(invitation.getLastSentTime()) : "-");
		
		identitiesPanel.setVisible(!invitation.getIdentities().isEmpty());
		identities.clearContents();
		invitation.getIdentities().values().forEach(e -> identities.addEntry(e));
		
		attributesPanel.setVisible(!invitation.getAttributes().isEmpty());
		attributes.clearContents();
		invitation.getAttributes().values().forEach(e -> attributes.addEntry(e));

		groupsPanel.setVisible(!invitation.getGroupSelections().isEmpty());
		groups.clearContents();
		invitation.getGroupSelections().entrySet().forEach(e -> {
			if (form.getGroupParams().size() <= e.getKey())
				return;
			GroupRegistrationParam gp = form.getGroupParams().get(e.getKey());
			if (gp == null)
				return;
			groups.addEntry(new AbstractMap.SimpleEntry<>(
					gp.getGroupPath(), 
					e.getValue()));
		});
	}
}
