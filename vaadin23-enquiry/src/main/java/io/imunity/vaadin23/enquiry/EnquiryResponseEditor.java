/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.enquiry;

import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.endpoint.common.forms.BaseRequestEditor;
import io.imunity.vaadin23.endpoint.common.forms.RegistrationLayoutsContainer;
import io.imunity.vaadin23.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilderV23;
import io.imunity.vaadin23.endpoint.common.plugins.attributes.AttributeHandlerRegistryV23;
import io.imunity.vaadin23.endpoint.common.plugins.credentials.CredentialEditorRegistryV23;
import io.imunity.vaadin23.endpoint.common.plugins.identities.IdentityEditorRegistryV23;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.forms.PrefilledSet;

import java.util.List;
import java.util.Map;

public class EnquiryResponseEditor extends BaseRequestEditor<EnquiryResponse>
{
	private final EnquiryForm enquiryForm;
	private final PrefilledSet prefilled;
	private final List<PolicyAgreementConfiguration> filteredPolicyAgreement;
	private final Map<String, Object> messageParams;
	
	public EnquiryResponseEditor(MessageSource msg, EnquiryForm form,
	                             RemotelyAuthenticatedPrincipal remotelyAuthenticated,
	                             IdentityEditorRegistryV23 identityEditorRegistry,
	                             CredentialEditorRegistryV23 credentialEditorRegistry,
	                             AttributeHandlerRegistryV23 attributeHandlerRegistry,
	                             AttributeTypeManagement atMan, CredentialManagement credMan,
	                             GroupsManagement groupsMan, NotificationPresenter notificationPresenter,
	                             PolicyAgreementRepresentationBuilderV23 policyAgreementsRepresentationBuilder,
	                             List<PolicyAgreementConfiguration> filteredPolicyAgreement,
	                             PrefilledSet prefilled,
	                             Map<String, Object> messageParams) throws Exception
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, atMan, credMan, groupsMan, notificationPresenter,
				policyAgreementsRepresentationBuilder);
		this.enquiryForm = form;
		this.filteredPolicyAgreement = filteredPolicyAgreement;
		this.prefilled = prefilled;
		this.messageParams = messageParams;

		validateMandatoryRemoteInput();
		initUI();
	}
	
	@Override
	public EnquiryResponse getRequest(boolean withCredentials) throws FormValidationException
	{
		EnquiryResponse ret = new EnquiryResponse();
		FormErrorStatus status = new FormErrorStatus();

		super.fillRequest(ret, status, withCredentials);
		
		if (status.hasFormException)
			throw new FormValidationException();
		
		return ret;
	}
	
	
	
	private void initUI()
	{
		RegistrationLayoutsContainer layoutContainer = createLayouts(messageParams);
		
		createControls(layoutContainer, enquiryForm.getEffectiveFormLayout(msg), prefilled);
	}

	@Override
	protected boolean isPolicyAgreementsIsFiltered(PolicyAgreementConfiguration toCheck)
	{
		return !filteredPolicyAgreement.contains(toCheck);
	}	
}


