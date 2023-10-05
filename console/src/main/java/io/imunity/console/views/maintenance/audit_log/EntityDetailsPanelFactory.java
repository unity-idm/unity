/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.maintenance.audit_log;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.FormLayoutLabel;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityScheduledOperation;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Collection;
import java.util.Map;

public class EntityDetailsPanelFactory
{
	public static FormLayout create(MessageSource msg, IdentityFormatter idFormatter, Entity entity, String label,
									Collection<GroupMembership> groups)
	{
		FormLayout formLayout = new FormLayout();
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		formLayout.addFormItem(new Span(label == null ? "[" + entity.getId() + "]" : label + " [" + entity.getId() + "]"),
				new FormLayoutLabel(msg.getMessage("IdentityDetails.id")));
		formLayout.addFormItem(new Span(msg.getMessage("EntityState." + entity.getState().toString())),
				new FormLayoutLabel(msg.getMessage("IdentityDetails.status")));

		EntityScheduledOperation operation = entity.getEntityInformation().getScheduledOperation();
		if (operation != null)
		{
			formLayout.addFormItem(
					new Span(msg.getMessage(
							"EntityScheduledOperationWithDate." + operation,
							entity.getEntityInformation().getScheduledOperationTime())
					),
					new FormLayoutLabel(msg.getMessage("IdentityDetails.expiration"))
			);

		}

		VerticalLayout identitiesLayout = new VerticalLayout();
		identitiesLayout.setPadding(false);
		formLayout.addFormItem(identitiesLayout, new FormLayoutLabel(msg.getMessage("IdentityDetails.identities")));
		for (Identity id : entity.getIdentities())
			identitiesLayout.add(new Html("<div>" + idFormatter.toString(id) + "</div>"));

		CredentialInfo credInf = entity.getCredentialInfo();
		formLayout.addFormItem(new Span(credInf.getCredentialRequirementId()),
				new FormLayoutLabel(msg.getMessage("IdentityDetails.credReq")));

		VerticalLayout credentialResetLayout = new VerticalLayout();
		credentialResetLayout.setPadding(false);
		FormLayout.FormItem credentialStatusItem = formLayout.addFormItem(credentialResetLayout,
				new FormLayoutLabel(msg.getMessage("IdentityDetails.credStatus")));
		for (Map.Entry<String, CredentialPublicInformation> cred : credInf.getCredentialsState().entrySet())
		{
			String status = msg.getMessage("CredentialStatus." +
					cred.getValue().getState().toString());
			credentialResetLayout.add(new Span(msg.getMessage("IdentityDetails.credStatusValue", cred.getKey(), status)));
		}
		credentialStatusItem.setVisible(!credInf.getCredentialsState().entrySet().isEmpty());


		VerticalLayout groupsLayout = new VerticalLayout();
		groupsLayout.setPadding(false);
		formLayout.addFormItem(groupsLayout, new FormLayoutLabel(msg.getMessage("IdentityDetails.groups")));
		for (GroupMembership groupM : groups)
			groupsLayout.add(new Html("<div>" + MembershipFormatter.toString(msg, groupM) + "</div>"));

		return formLayout;
	}
}
