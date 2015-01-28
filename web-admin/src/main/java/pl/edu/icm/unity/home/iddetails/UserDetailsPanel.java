/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Collection;
import java.util.Map;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Label;

/**
 * Presents a basic information about a single entity. No editing is possible. Targeted for presentation
 * to ordinary user.
 * 
 * @author K. Benedyczak
 */
public class UserDetailsPanel
{
	private UnityMessageSource msg;
	private Label id;
	private Label scheduledAction;
	private HtmlLabel identities;
	private HtmlLabel credStatus;
	private HtmlLabel groups;
	
	
	public UserDetailsPanel(UnityMessageSource msg)
	{
		this.msg = msg;
		id = new Label();
		id.setCaption(msg.getMessage("IdentityDetails.id"));

		scheduledAction = new Label();
		scheduledAction.setCaption(msg.getMessage("IdentityDetails.expiration"));
		
		identities = new HtmlLabel(msg);
		identities.setCaption(msg.getMessage("IdentityDetails.identities"));

		credStatus = new HtmlLabel(msg);
		credStatus.setCaption(msg.getMessage("IdentityDetails.credStatus"));

		groups = new HtmlLabel(msg);
		groups.setCaption(msg.getMessage("IdentityDetails.groups"));
		
		
	}
	
	public void addIntoLayout(AbstractOrderedLayout layout)
	{
		layout.addComponents(id, scheduledAction, identities, credStatus, groups);
	}
	
	public void setInput(EntityWithLabel entityWithLabel, Collection<Group> groups)
	{
		id.setValue(entityWithLabel.toString());
		Entity entity = entityWithLabel.getEntity();
		
		EntityScheduledOperation operation = entity.getEntityInformation().getScheduledOperation();
		if (operation != null)
		{
			scheduledAction.setVisible(true);
			String action = msg.getMessage("EntityScheduledOperationWithDate." + operation.toString(), 
					entity.getEntityInformation().getScheduledOperationTime());
			scheduledAction.setValue(action);
		} else
		{
			scheduledAction.setVisible(false);
		}
		
		identities.resetValue();
		for (Identity id: entity.getIdentities())
		{
			identities.addHtmlValueLine("IdentityFormatter.identityCore", id.getTypeId(), 
					id.getType().getIdentityTypeProvider().toPrettyStringNoPrefix(id.getValue()));
		}
		
		CredentialInfo credInf = entity.getCredentialInfo();
		
		credStatus.resetValue();
		for (Map.Entry<String, CredentialPublicInformation> cred: credInf.getCredentialsState().entrySet())
		{
			String status = msg.getMessage("CredentialStatus." + 
					cred.getValue().getState().toString());
			credStatus.addHtmlValueLine("IdentityDetails.credStatusValue", cred.getKey(), status);
		}
		
		this.groups.resetValue();
		for (Group group: groups)
		{
			this.groups.addHtmlValueLine("IdentityDetails.groupLine", 
					group.getDisplayedName().getValue(msg));
		}
	}
}
