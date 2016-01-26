/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;

import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.DateField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Edit UI for {@link InvitationParam}.
 * @author Krzysztof Benedyczak
 */
public class InvitationEditor extends CustomComponent
{
	public static final long DEFAULT_TTL = 1000 * 24 * 3600 * 3; //3 days
	private UnityMessageSource msg;
	
	private ComboBox forms;
	private DateField expiration;
	private TextField contactAddress;
	private ComboBox channelId;
	
	public InvitationEditor(UnityMessageSource msg, Collection<RegistrationForm> availableForms,
			Collection<String> channels) throws WrongArgumentException
	{
		this(msg, availableForms, channels, null);
	}

	public InvitationEditor(UnityMessageSource msg, Collection<RegistrationForm> availableForms,
			Collection<String> channels, InvitationParam toEdit) throws WrongArgumentException
	{
		this.msg = msg;
		initUI(toEdit, availableForms, channels);
	}

	private void initUI(InvitationParam toEdit, Collection<RegistrationForm> availableForms,
			Collection<String> channels) throws WrongArgumentException
	{
		forms = new ComboBox(msg.getMessage("InvitationViewer.formId"));
		forms.setNewItemsAllowed(false);
		forms.setNullSelectionAllowed(false);
		availableForms.stream()
			.filter(form -> form.getRegistrationCode() == null && form.isPubliclyAvailable())
			.forEach(form -> forms.addItem(form.getName()));

		if (forms.getItemIds().isEmpty())
			throw new WrongArgumentException("There are no public registration forms to create an invitation for.");
		
		expiration = new DateField(msg.getMessage("InvitationViewer.expiration"));
		expiration.setRequired(true);
		expiration.setResolution(Resolution.MINUTE);
		
		channelId = new ComboBox(msg.getMessage("InvitationViewer.channelId"));
		channelId.addItems(channels);
		
		contactAddress = new TextField(msg.getMessage("InvitationViewer.contactAddress"));
		
		FormLayout main = new FormLayout();
		main.addComponents(forms, expiration, channelId, contactAddress);
		setCompositionRoot(main);
		
		if (toEdit != null)
		{
			forms.setValue(toEdit.getFormId());
			expiration.setValue(new Date(toEdit.getExpiration().getEpochSecond()*1000));
			if (toEdit.getContactAddress() != null)
				contactAddress.setValue(toEdit.getContactAddress());
			if (toEdit.getChannelId() != null)
				channelId.setValue(toEdit.getChannelId());
		} else
		{
			forms.setValue(forms.getItemIds().iterator().next());
			expiration.setValue(new Date(System.currentTimeMillis() + DEFAULT_TTL));
		}
	}
	
	public InvitationParam getInvitation() throws FormValidationException
	{
		String addr = contactAddress.isEmpty() ? null : contactAddress.getValue();
		String channel = channelId.isEmpty() ? null : (String)channelId.getValue();
		InvitationParam ret = new InvitationParam((String)forms.getValue(), 
				Instant.ofEpochMilli(expiration.getValue().getTime()),
				addr,
				channel);
		//TODO fill rest
		return ret;
	}
}
