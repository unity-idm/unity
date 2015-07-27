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
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.identities.IdentityFormatter;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabel;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

/**
 * Presents a complete and comprehensive information about a single entity. No editing is possible.
 * Targeted for admin user.
 * @author K. Benedyczak
 */
public class EntityDetailsPanel extends FormLayout
{
	private UnityMessageSource msg;
	private Label id;
	private Label status;
	private Label scheduledAction;
	private ListOfElements<String> identities;
	private Label credReq;
	private HtmlLabel credStatus;
	private HtmlLabel groups;
	
	
	public EntityDetailsPanel(UnityMessageSource msg)
	{
		this.msg = msg;
		id = new Label();
		id.setCaption(msg.getMessage("IdentityDetails.id"));

		status = new Label();
		status.setCaption(msg.getMessage("IdentityDetails.status"));
		
		scheduledAction = new Label();
		scheduledAction.setCaption(msg.getMessage("IdentityDetails.expiration"));
		
		identities = new ListOfElements<>(msg, new ListOfElements.LabelConverter<String>()
		{
			@Override
			public Label toLabel(String value)
			{
				return new HtmlSimplifiedLabel(value);
			}
		});
		identities.setAddSeparatorLine(false);
		identities.setCaption(msg.getMessage("IdentityDetails.identities"));

		credReq = new Label();
		credReq.setCaption(msg.getMessage("IdentityDetails.credReq"));

		credStatus = new HtmlLabel(msg);
		credStatus.setCaption(msg.getMessage("IdentityDetails.credStatus"));

		groups = new HtmlLabel(msg);
		groups.setCaption(msg.getMessage("IdentityDetails.groups"));
		
		addComponents(id, status, scheduledAction, identities, credReq, credStatus, groups);
	}
	
	public void setInput(EntityWithLabel entityWithLabel, Collection<String> groups)
	{
		id.setValue(entityWithLabel.toString());
		Entity entity = entityWithLabel.getEntity();
		
		status.setValue(msg.getMessage("EntityState." + entity.getState().toString()));
		
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
		
		identities.clearContents();
		for (Identity id: entity.getIdentities())
			identities.addEntry(IdentityFormatter.toString(msg, id));
		
		CredentialInfo credInf = entity.getCredentialInfo();
		credReq.setValue(credInf.getCredentialRequirementId());
		
		credStatus.resetValue();
		for (Map.Entry<String, CredentialPublicInformation> cred: credInf.getCredentialsState().entrySet())
		{
			String status = msg.getMessage("CredentialStatus." + 
					cred.getValue().getState().toString());
			credStatus.addHtmlValueLine("IdentityDetails.credStatusValue", cred.getKey(), status);
		}
		credStatus.setVisible(!credInf.getCredentialsState().entrySet().isEmpty());
			
		
		this.groups.resetValue();
		for (String group: groups)
		{
			this.groups.addHtmlValueLine("IdentityDetails.groupLine", group);
		}
	}
}
