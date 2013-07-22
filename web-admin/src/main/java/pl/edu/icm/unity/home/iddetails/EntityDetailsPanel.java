/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Collection;
import java.util.Map;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

/**
 * Presents a complete and comprehensive information about a single entity. No editing is possible.
 * @author K. Benedyczak
 */
public class EntityDetailsPanel extends FormLayout
{
	private UnityMessageSource msg;
	private Label id;
	private Label status;
	private Label identities;
	private Label credReq;
	private Label credStatus;
	private Label groups;
	
	
	public EntityDetailsPanel(UnityMessageSource msg)
	{
		this.msg = msg;
		id = new Label();
		id.setCaption(msg.getMessage("IdentityDetails.id"));

		status = new Label();
		status.setCaption(msg.getMessage("IdentityDetails.status"));
		
		identities = new Label();
		identities.setContentMode(ContentMode.HTML);
		identities.setCaption(msg.getMessage("IdentityDetails.identities"));

		credReq = new Label();
		credReq.setCaption(msg.getMessage("IdentityDetails.credReq"));

		credStatus = new Label();
		credStatus.setCaption(msg.getMessage("IdentityDetails.credStatus"));
		credStatus.setContentMode(ContentMode.HTML);

		groups = new Label();
		groups.setCaption(msg.getMessage("IdentityDetails.groups"));
		groups.setContentMode(ContentMode.HTML);
		
		addComponents(id, status, identities, credReq, credStatus, groups);
	}
	
	public void setInput(Entity entity, Collection<String> groups)
	{
		id.setValue(String.valueOf(entity.getId()));
		
		status.setValue(msg.getMessage("EntityState." + entity.getState().toString()));
		
		StringBuilder sb = new StringBuilder();
		for (Identity id: entity.getIdentities())
		{
			String local = id.isLocal() ? msg.getMessage("IdentityDetails.identityLocal") : 
				msg.getMessage("IdentityDetails.identityRemote");
			sb.append(msg.getMessage("IdentityDetails.identity", local, id.getTypeId(), 
					id.getType().getIdentityTypeProvider().toPrettyStringNoPrefix(id.getValue())));
			sb.append("<br>");
		}
		identities.setValue(sb.toString());
		
		CredentialInfo credInf = entity.getCredentialInfo();
		credReq.setValue(credInf.getCredentialRequirementId());
		
		sb = new StringBuilder();
		for (Map.Entry<String, LocalCredentialState> cred: credInf.getCredentialsState().entrySet())
		{
			sb.append(cred.getKey() + ": " + msg.getMessage("CredentialStatus."+cred.getValue().toString()));
			sb.append("<br>");
		}
		credStatus.setValue(sb.toString());
		
		sb = new StringBuilder();
		for (String group: groups)
		{
			sb.append(group);
			sb.append("<br>");
		}
		this.groups.setValue(sb.toString());
	}
}
