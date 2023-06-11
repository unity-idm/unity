/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pl.edu.icm.unity.base.db.DBDumpContentElements;
import pl.edu.icm.unity.store.impl.attribute.AttributeIE;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesIE;
import pl.edu.icm.unity.store.impl.audit.AuditEventIE;
import pl.edu.icm.unity.store.impl.entities.EntityIE;
import pl.edu.icm.unity.store.impl.files.FileIE;
import pl.edu.icm.unity.store.impl.groups.GroupIE;
import pl.edu.icm.unity.store.impl.identities.IdentityIE;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypeIE;
import pl.edu.icm.unity.store.impl.idpStatistics.IdpStatisticIE;
import pl.edu.icm.unity.store.impl.membership.MembershipIE;
import pl.edu.icm.unity.store.impl.messages.MessageIE;
import pl.edu.icm.unity.store.impl.policyDocuments.PolicyDocucentIE;
import pl.edu.icm.unity.store.impl.tokens.TokensIE;
import pl.edu.icm.unity.store.objstore.ac.AttributeClassHandler;
import pl.edu.icm.unity.store.objstore.authn.AuthenticatorConfigurationHandler;
import pl.edu.icm.unity.store.objstore.authnFlow.AuthenticationFlowHandler;
import pl.edu.icm.unity.store.objstore.bulk.ProcessingRuleHandler;
import pl.edu.icm.unity.store.objstore.cert.CertificateHandler;
import pl.edu.icm.unity.store.objstore.cred.CredentialHandler;
import pl.edu.icm.unity.store.objstore.credreq.CredentialRequirementHandler;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;
import pl.edu.icm.unity.store.objstore.msgtemplate.MessageTemplateHandler;
import pl.edu.icm.unity.store.objstore.notify.NotificationChannelHandler;
import pl.edu.icm.unity.store.objstore.realm.RealmHandler;
import pl.edu.icm.unity.store.objstore.reg.eform.EnquiryFormHandler;
import pl.edu.icm.unity.store.objstore.reg.eresp.EnquiryResponseHandler;
import pl.edu.icm.unity.store.objstore.reg.form.RegistrationFormHandler;
import pl.edu.icm.unity.store.objstore.reg.invite.InvitationHandler;
import pl.edu.icm.unity.store.objstore.reg.req.RegistrationRequestHandler;
import pl.edu.icm.unity.store.objstore.tprofile.InputTranslationProfileHandler;
import pl.edu.icm.unity.store.objstore.tprofile.OutputTranslationProfileHandler;

/**
 * Helper which maps from {@link DBDumpContentElements} to database elements list.
 * 
 * @author P.Piernik
 *
 */
public class DBDumpContentTypeMapper
{
	public static List<String> getDBElements(DBDumpContentElements content)
	{
		List<String> ret = new ArrayList<>();

		if (content.systemConfig)
		{
			ret.addAll(getSystemConfigElements());
		}

		if (content.directorySchema)
		{
			ret.addAll(getDirSchemaElements());
		}

		if (content.users)
		{
			if (!content.directorySchema && !ret.containsAll(getDirSchemaElements()))
			{
				ret.addAll(getDirSchemaElements());
			}

			ret.addAll(getUsersElements());
		}

		if (content.signupRequests)
		{
			if (!content.directorySchema && !ret.containsAll(getDirSchemaElements()))
			{
				ret.addAll(getDirSchemaElements());
			}

			ret.addAll(getSignupRequestsElements());
		}

		if (content.auditLogs)
		{
			ret.addAll(getAuditLogsElements());
		}
		
		if (content.idpStatistics)
		{
			ret.addAll(getIdpStatisticsElements());
		}

		return ret;
	}

	public static List<String> getElementsForClearDB(DBDumpContentElements content)
	{
		List<String> ret = getDBElements(content);

		if (content.directorySchema && !ret.containsAll(getUsersElements()))
		{
			ret.addAll(getUsersElements());
		}

		if (content.directorySchema && !ret.containsAll(getSignupRequestsElements()))
		{
			ret.addAll(getSignupRequestsElements());
		}

		if (content.users && !ret.containsAll(getDirSchemaElements()))
		{
			ret.addAll(getDirSchemaElements());
		}

		if (content.signupRequests && !ret.containsAll(getDirSchemaElements()))
		{
			ret.addAll(getDirSchemaElements());
		}
		
		if (content.idpStatistics)
		{
			ret.addAll(getIdpStatisticsElements());
		}

		return ret;
	}

	public static List<String> getSystemConfigElements()
	{
		return Arrays.asList(CredentialHandler.CREDENTIAL_OBJECT_TYPE, RealmHandler.REALM_OBJECT_TYPE,
				AuthenticatorConfigurationHandler.AUTHENTICATOR_OBJECT_TYPE,
				AuthenticationFlowHandler.AUTHENTICATION_FLOW_OBJECT_TYPE,
				EndpointHandler.ENDPOINT_OBJECT_TYPE, FileIE.FILES_OBJECT_TYPE,
				InputTranslationProfileHandler.TRANSLATION_PROFILE_OBJECT_TYPE,
				CertificateHandler.CERTIFICATE_OBJECT_TYPE,
				NotificationChannelHandler.NOTIFICATION_CHANNEL_ID,
				OutputTranslationProfileHandler.TRANSLATION_PROFILE_OBJECT_TYPE,
				PolicyDocucentIE.POLICY_DOCUMENTS_OBJECT_TYPE,
				MessageIE.MESSAGES_OBJECT_TYPE);

	}

	public static List<String> getDirSchemaElements()
	{
		return Arrays.asList(CredentialRequirementHandler.CREDENTIAL_REQ_OBJECT_TYPE,
				ProcessingRuleHandler.PROCESSING_RULE_OBJECT_TYPE,
				AttributeTypesIE.ATTRIBUTES_TYPE_OBJECT_TYPE,
				EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE, GroupIE.GROUPS_OBJECT_TYPE,
				IdentityTypeIE.IDENTITY_TYPE_OBJECT_TYPE,
				AttributeClassHandler.ATTRIBUTE_CLASS_OBJECT_TYPE,
				RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE,
				MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE);

	}

	public static List<String> getUsersElements()
	{
		return Arrays.asList(IdentityIE.IDENTITIES_OBJECT_TYPE, EntityIE.ENTITIES_OBJECT_TYPE,
				AttributeIE.ATTRIBUTES_OBJECT_TYPE, MembershipIE.GROUP_MEMBERS_OBJECT_TYPE,
				TokensIE.TOKEN_OBJECT_TYPE);
	}

	public static List<String> getSignupRequestsElements()
	{
		return Arrays.asList(InvitationHandler.INVITATION_OBJECT_TYPE,
				EnquiryResponseHandler.ENQUIRY_RESPONSE_OBJECT_TYPE,
				RegistrationRequestHandler.REGISTRATION_REQUEST_OBJECT_TYPE);
	}

	public static List<String> getAuditLogsElements()
	{
		return Arrays.asList(AuditEventIE.AUDIT_EVENTS_OBJECT_TYPE);
	}

	public static List<String> getIdpStatisticsElements()
	{
		return Arrays.asList(IdpStatisticIE.IDP_STATISTIC_OBJECT_TYPE);

	}
}
