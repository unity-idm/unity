/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.requests;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.NativeLabel;

import io.imunity.console.views.maintenance.audit_log.IdentityFormatter;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Shows request contents and provides a possibility to edit it.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
class RegistrationReviewPanel extends RequestReviewPanelBase
{
	private RegistrationRequestState requestState;
	private NativeLabel code;
	
	@Autowired
	RegistrationReviewPanel(MessageSource msg, AttributeHandlerRegistry handlersRegistry,
			IdentityTypesRegistry idTypesRegistry, IdentityFormatter idFormatter, GroupsManagement groupMan,
			PolicyDocumentManagement policyDocMan,
			PolicyAgreementRepresentationBuilder policyAgreementRepresentationBuilder)
	{
		super(msg, handlersRegistry, idTypesRegistry, idFormatter, groupMan, policyDocMan,
				policyAgreementRepresentationBuilder);

		initUI();
	}

	private void initUI()
	{
		setMargin(false);
		setPadding(false);
		code = new NativeLabel(msg.getMessage("RequestReviewPanel.codeValid"));
		code.addClassName(CssClassNames.BOLD.getName());
		add(code);
		addStandardComponents(msg.getMessage("RequestReviewPanel.requestedGroups"));
	}
	
	RegistrationRequest getUpdatedRequest()
	{
		RegistrationRequest ret = new RegistrationRequest();
		fillRequest(ret);
		RegistrationRequest orig = requestState.getRequest();
		ret.setRegistrationCode(orig.getRegistrationCode());
		return ret;
	}
	
	void setInput(RegistrationRequestState requestState, RegistrationForm form)
	{
		this.requestState = requestState;
		super.setInput(requestState, form);
		code.setVisible(requestState.getRequest().getRegistrationCode() != null);
		setGroupEntries(getGroupEntries(requestState, form));	
	}

	private List<Component> getGroupEntries(RegistrationRequestState requestState, RegistrationForm form)
	{
		return super.getGroupEntries(requestState, form, Arrays.asList(), false);
	}
}
