/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Collection;
import java.util.Map;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.common.EntityWithLabel;

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
	private boolean showAdminData;
	private Label id;
	private Label status;
	private Label identities;
	private Label credReq;
	private Label credStatus;
	private Label groups;
	
	
	public EntityDetailsPanel(UnityMessageSource msg, boolean showAdminData)
	{
		this.msg = msg;
		this.showAdminData = showAdminData;
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
	
	public void setInput(EntityWithLabel entityWithLabel, Collection<String> groups)
	{
		id.setValue(entityWithLabel.toString());
		Entity entity = entityWithLabel.getEntity();
		
		status.setValue(msg.getMessage("EntityState." + entity.getState().toString()));
		
		StringBuilder sb = new StringBuilder();
		for (Identity id: entity.getIdentities())
		{
			if (!showAdminData || id.isLocal())
			{
				sb.append(msg.getMessage("IdentityDetails.identityLocal", id.getTypeId(), 
					id.getType().getIdentityTypeProvider().toPrettyStringNoPrefix(id.getValue())));
			} else
			{
				String trProfile = id.getTranslationProfile() == null ? 
						"-" : id.getTranslationProfile(); 
				String created = msg.getMessageNullArg("IdentityDetails.creationDate", id.getCreationTs());
				String updated = msg.getMessageNullArg("IdentityDetails.updatedDate", id.getUpdateTs());
				sb.append(msg.getMessage("IdentityDetails.identityRemote", id.getTypeId(), 
						id.getRemoteIdp(), trProfile, 
						id.getType().getIdentityTypeProvider().toPrettyStringNoPrefix(id.getValue())));
				sb.append(created + updated);
			}
			sb.append("<br>");
		}
		identities.setValue(sb.toString());
		
		CredentialInfo credInf = entity.getCredentialInfo();
		credReq.setValue(credInf.getCredentialRequirementId());
		
		sb = new StringBuilder();
		for (Map.Entry<String, CredentialPublicInformation> cred: credInf.getCredentialsState().entrySet())
		{
			sb.append(cred.getKey() + ": " + msg.getMessage("CredentialStatus." + 
					cred.getValue().getState().toString()));
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
