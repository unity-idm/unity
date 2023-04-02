/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.capacityLimit.CapacityLimit;
import pl.edu.icm.unity.base.capacityLimit.CapacityLimitName;
import pl.edu.icm.unity.base.event.PersistableEvent;
import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic.SigInStatistic;
import pl.edu.icm.unity.msg.Message;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.authn.ExpectedIdentity.IdentityExpectation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.GroupProperty;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.types.basic.NotificationChannel;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.invite.ComboInvitationParam;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class MapperTest
{
	@Test
	public void test() throws JsonProcessingException
	{

		Identity id1 = new Identity("email", "test@wp.pl", 1L, "test@wp.pl");
		ConfirmationInfo confirmationInfo = new ConfirmationInfo(true);
		confirmationInfo.setSentRequestAmount(1);
		id1.setConfirmationInfo(confirmationInfo);
		id1.setRealm("real");
		id1.setRemoteIdp("remoteIdp");
		id1.setTarget("target");
		id1.setTranslationProfile("Profile");
		ObjectNode meta = Constants.MAPPER.createObjectNode();
		meta.put("1", "v");
		id1.setMetadata(meta);

		EntityInformation en1 = new EntityInformation(1);
		en1.setRemovalByUserTime(new Date());
		en1.setScheduledOperationTime(new Date());
		en1.setScheduledOperation(EntityScheduledOperation.DISABLE);

		CredentialPublicInformation credentialPublicInformation = new CredentialPublicInformation(
				LocalCredentialState.correct, "state", "state");
		CredentialInfo credentialInfo = new CredentialInfo("credreq1", Map.of("test", credentialPublicInformation));

		Entity en = new Entity(List.of(id1), en1, credentialInfo);

		// System.out.println(Constants.MAPPER.writeValueAsString(en1));

		Attribute attr = new Attribute("attr", "syntax", "/A", Lists.newArrayList("v1", "v2"), "remoteIdp",
				"translationProfile");
		AttributeExt a = new AttributeExt(attr, true, new Date(100), new Date(1000));

		// System.out.println(Constants.MAPPER.writeValueAsString(a));

		Group group = new Group("/A/B/C");
		group.setPublic(false);
		group.setAttributesClasses(Set.of("attrClass1", "attrClass2"));
		group.setDisplayedName(new I18nString("groupDisp"));
		group.setAttributeStatements(new AttributeStatement[]
		{ new AttributeStatement("cond", "/extra", ConflictResolution.merge,
				new Attribute("attr", "string", "/", List.of("v1"))) });
		group.setProperties(List.of(new GroupProperty("key", "value")));
		group.setDescription(new I18nString("desc"));

		I18nString tt = new I18nString("default");
		tt.addValue("pl", "plval");
		tt.addValue("en", "enval");

		// System.out.println(Constants.MAPPER.writeValueAsString(tt));

		GroupProperty groupProperty = new GroupProperty("key", "val");
		// System.out.println(Constants.MAPPER.writeValueAsString(groupProperty));

		GroupDelegationConfiguration groupDelegationConfiguration = new GroupDelegationConfiguration(true, true,
				"logoUrl", "reg", "enq", "menq", List.of("attr1", "attr2"));
		// System.out.println(Constants.MAPPER.writeValueAsString(groupDelegationConfiguration));

		Attribute attr1 = new Attribute("attr", "string", "/", List.of("v1", "v2"), "remIdP", "profile");

//		System.out.println(Constants.MAPPER.writeValueAsString(attr1));

		GroupMembership groupMembership = new GroupMembership("/", 1, new Date(1));
		groupMembership.setTranslationProfile("profile");
		groupMembership.setRemoteIdp("remoteIdp");
//		System.out.println(Constants.MAPPER.writeValueAsString(groupMembership));

//		
//		ExternalizedAttribute externalizedAttribute = new ExternalizedAttribute(a, List.of("v1s" ,"v2s"));
//
//		
//		EntityWithAttributes entityWithAttributes = new EntityWithAttributes(en, Map.of("/", groupMembership), Map.of("/", List.of(externalizedAttribute)));
//		
		// System.out.println(Constants.MAPPER.writeValueAsString(entityWithAttributes));

		GroupContents groupContents = new GroupContents();
		groupContents.setGroup(group);
		groupContents.setSubGroups(List.of("/A", "/B"));
		groupContents.setMembers(List.of(groupMembership));
		// System.out.println(Constants.MAPPER.writeValueAsString(groupContents));

		AttributeType attributeType = new AttributeType("name", "string");
		attributeType.setDescription(new I18nString("desc"));
		attributeType.setDisplayedName(new I18nString("disp"));
		attributeType.setFlags(2);
		attributeType.setMaxElements(10);
		attributeType.setGlobal(true);
		attributeType.setMinElements(2);
		attributeType.setSelfModificable(true);
		attributeType.setValueSyntaxConfiguration(Constants.MAPPER.createObjectNode());
		attributeType.setMetadata(Map.of("m1", "v1"));
		attributeType.setUniqueValues(true);
//		System.out.println(Constants.MAPPER.writeValueAsString(attributeType));

		AuthenticationRealm authenticationRealm = new AuthenticationRealm("realm", "desc", 1, 1,
				RememberMePolicy.allowFor2ndFactor, 1, 2);

		// System.out.println(Constants.MAPPER.writeValueAsString(authenticationRealm));

		EndpointTypeDescription endpointTypeDescription = new EndpointTypeDescription("name", "desc", "binding",
				Map.of("p1k", "p1v"));

		Properties prop = new Properties();
		prop.put("key", "val");
		// System.out.println(Constants.MAPPER.writeValueAsString(endpointTypeDescription));

		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(new I18nString("disp"), "desc",
				List.of("ao1"), "conf", "realm", "tag1");
		// System.out.println(Constants.MAPPER.writeValueAsString(endpointConfiguration));

		Endpoint endpoint = new Endpoint("endpoint", "rest", "/rest", endpointConfiguration, 1);
		// System.out.println(Constants.MAPPER.writeValueAsString(endpoint));

		ResolvedEndpoint resolvedEndpoint = new ResolvedEndpoint(endpoint, authenticationRealm,
				endpointTypeDescription);
		// System.out.println(Constants.MAPPER.writeValueAsString(resolvedEndpoint));

		Token token = new Token("tokenType", "tokenValue", 1L);
		token.setContents("content".getBytes());
		token.setCreated(new Date(100));
		// System.out.println(Constants.MAPPER.writeValueAsString(token));

		TranslationAction translationAction = new TranslationAction("action", "p1", "p2");
		// System.out.println(Constants.MAPPER.writeValueAsString(translationAction));

		TranslationRule translationRule = new TranslationRule("true", translationAction);
		// System.out.println(Constants.MAPPER.writeValueAsString(translationRule));

		TranslationProfile translationProfile = new TranslationProfile("name", "desc", ProfileType.REGISTRATION,
				List.of(translationRule));
		// System.out.println(Constants.MAPPER.writeValueAsString(translationProfile));

		IdentityParam idParam1 = new IdentityParam("email", "test@wp.pl", "remoteIdp", "Profile");
		confirmationInfo.setSentRequestAmount(1);
		idParam1.setConfirmationInfo(confirmationInfo);
		idParam1.setRealm("realm");
		idParam1.setTarget("target");
		idParam1.setMetadata(meta);

//		System.out.println(Constants.MAPPER.writeValueAsString(idParam1));

		CredentialParamValue credentialParamValue = new CredentialParamValue("credential", "secret");
//		System.out.println(Constants.MAPPER.writeValueAsString(credentialParamValue));

		GroupSelection groupSelection = new GroupSelection(List.of("/g1", "/g2"));
		groupSelection.setExternalIdp("externalIdp");
		groupSelection.setTranslationProfile("Profile");

		Selection selection = new Selection(true, "externalIdp", "Profile");

		PolicyAgreementDecision policyAgreementDecision = new PolicyAgreementDecision(
				PolicyAgreementAcceptanceStatus.ACCEPTED, List.of(1L, 2L));

		// System.out.println(Constants.MAPPER.writeValueAsString(policyAgreementDecision));

		RegistrationRequest registrationRequest = new RegistrationRequest();
		registrationRequest.setAgreements(List.of(new Selection(true, "externalIdp", "profile")));
		registrationRequest.setAttributes(List.of(new Attribute("attr", "syntax", "/", List.of("v1"))));
		registrationRequest.setComments("comments");
		registrationRequest.setCredentials(List.of(credentialParamValue));
		registrationRequest.setFormId("formId");
		registrationRequest.setGroupSelections(List.of(groupSelection));
		registrationRequest.setIdentities(List.of(idParam1));
		registrationRequest.setPolicyAgreements(List.of(policyAgreementDecision));
		registrationRequest.setRegistrationCode("Code");
		registrationRequest.setUserLocale("en");
//		System.out.println(Constants.MAPPER.writeValueAsString(registrationRequest));

		AdminComment adminComment = new AdminComment("comment", 1, true);

		// System.out.println(Constants.MAPPER.writeValueAsString(adminComment));

		RegistrationContext registrationContext = new RegistrationContext(true, TriggeringMode.manualAtLogin);

		// System.out.println(Constants.MAPPER.writeValueAsString(registrationContext));

		RegistrationRequestState registrationRequestState = new RegistrationRequestState();
		registrationRequestState.setAdminComments(List.of(adminComment));
		registrationRequestState.setCreatedEntityId(1L);
		registrationRequestState.setRegistrationContext(registrationContext);
		registrationRequestState.setRequest(registrationRequest);
		registrationRequestState.setStatus(RegistrationRequestStatus.pending);
		registrationRequestState.setTimestamp(new Date(1));
		registrationRequestState.setRequestId("id");

		// System.out.println(Constants.MAPPER.writeValueAsString(registrationRequestState));

		ExpectedIdentity expectedIdentity = new ExpectedIdentity("identity", IdentityExpectation.MANDATORY);

		// System.out.println(Constants.MAPPER.writeValueAsString(expectedIdentity));

		FormPrefill formPrefill = new FormPrefill(FormType.REGISTRATION);
		formPrefill.setAllowedGroups(Map.of(1, groupSelection));
		formPrefill.setAttributes(Map.of(1, new PrefilledEntry<Attribute>(attr, PrefilledEntryMode.READ_ONLY)));
		formPrefill.setFormId("formId");
		formPrefill.setGroupSelections(
				Map.of(1, new PrefilledEntry<GroupSelection>(groupSelection, PrefilledEntryMode.READ_ONLY)));
		formPrefill.setIdentities(Map.of(1, new PrefilledEntry<IdentityParam>(idParam1, PrefilledEntryMode.HIDDEN)));
		formPrefill.setMessageParams(Map.of("mpk1", "mpv1"));

		ObjectNode formPrefillNode = Constants.MAPPER.createObjectNode();
		// formPrefill.toJson(formPrefillNode);

		FormPrefill formPrefill2 = new FormPrefill(FormType.REGISTRATION);
		formPrefill2.setAllowedGroups(Map.of(1, groupSelection));
		formPrefill2.setAttributes(Map.of(1, new PrefilledEntry<Attribute>(attr, PrefilledEntryMode.READ_ONLY)));
		formPrefill2.setFormId("formId2");
		formPrefill2.setGroupSelections(
				Map.of(1, new PrefilledEntry<GroupSelection>(groupSelection, PrefilledEntryMode.READ_ONLY)));
		formPrefill2.setIdentities(Map.of(1, new PrefilledEntry<IdentityParam>(idParam1, PrefilledEntryMode.HIDDEN)));
		formPrefill2.setMessageParams(Map.of("mpk1", "mpv1"));

		// ObjectNode formPrefillNode = Constants.MAPPER.createObjectNode();
		// formPrefill.toJson(formPrefillNode);

		// System.out.println(Constants.MAPPER.writeValueAsString(formPrefillNode));

//		RegistrationInvitationParam registrationInvitationParam = new RegistrationInvitationParam("formId",
//				Instant.ofEpochMilli(1));
//		registrationInvitationParam.setExpectedIdentity(expectedIdentity);
//		registrationInvitationParam.setFormPrefill(formPrefill);
//		registrationInvitationParam.setInviterEntity(Optional.of(1L));

		RegistrationInvitationParam registrationInvitationParam = RegistrationInvitationParam.builder()
				.withContactAddress("contactAddress")
				.withExpiration(Instant.ofEpochMilli(1))
				.withForm(formPrefill)
				.withInviter(1L)
				.withExpectedIdentity(expectedIdentity)
				.build();

		EnquiryInvitationParam enquiryInvitationParam = EnquiryInvitationParam.builder()
				.withContactAddress("contactAddress")
				.withExpiration(Instant.ofEpochMilli(1))
				.withForm(formPrefill)
				.withInviter(1L)
				.withEntity(1L)
				.build();

		ComboInvitationParam comboInvitationParam = ComboInvitationParam.builder()
				.withContactAddress("contactAddress")
				.withExpiration(Instant.ofEpochMilli(1))
				.withRegistrationForm(formPrefill)
				.withEnquiryForm(formPrefill2)
				.withInviter(1L)
				.build();

		InvitationWithCode invitationWithCode = new InvitationWithCode(registrationInvitationParam, "code",
				Instant.ofEpochMilli(1), 1);

//		System.out.println(Constants.MAPPER.writeValueAsString(invitationWithCode));

		SigInStatistic sigInStatistic = new SigInStatistic(
				LocalDateTime.ofInstant(Instant.ofEpochMilli(1), ZoneId.of("UTC")),
				LocalDateTime.ofInstant(Instant.ofEpochMilli(999999), ZoneId.of("UTC")), 1, 2, 3);

		// System.out.println(Constants.MAPPER.writeValueAsString(sigInStatistic));

		GroupedIdpStatistic groupedIdpStatistic = new GroupedIdpStatistic("idpId", "name", "client", "clientN",
				List.of(sigInStatistic));
		// System.out.println(Constants.MAPPER.writeValueAsString(groupedIdpStatistic));

		PersistableEvent persistableEvent = new PersistableEvent("trigger", 1L, Date.from(Instant.ofEpochMilli(1)),
				"content");
		// System.out.println(Constants.MAPPER.writeValueAsString(persistableEvent));

//		GroupMember groupMember = new GroupMember("/", en, List.of(a));
//		
//		
//		
//		EntityGroupAttributes entityGroupAttributes = new EntityGroupAttributes(1, List.of(a));
//		
//		
//		MultiGroupMembers multiGroupMembers = new MultiGroupMembers(List.of(en), Map.of("/", List.of(entityGroupAttributes)));

		// System.out.println(Constants.MAPPER.writeValueAsString(multiGroupMembers));

		EmailConfirmationConfiguration emailConfirmationConfiguration = new EmailConfirmationConfiguration("template");
		emailConfirmationConfiguration.setValidityTime(10);

		IdentityType identityType = new IdentityType("type");
		identityType.setEmailConfirmationConfiguration(emailConfirmationConfiguration);
		identityType.setDescription("desc");
		identityType.setIdentityTypeProvider("typeProvider");
		identityType.setIdentityTypeProviderSettings("providerSettings");
		identityType.setMaxInstances(1);
		identityType.setMinInstances(1);
		identityType.setMinVerifiedInstances(2);
		identityType.setSelfModificable(true);

		StoredPolicyDocument storedPolicyDocument = new StoredPolicyDocument(1L, "name");
		storedPolicyDocument.setContent(new I18nString("content"));
		storedPolicyDocument.setContentType(PolicyDocumentContentType.EMBEDDED);
		storedPolicyDocument.setDisplayedName(new I18nString("dispName"));
		storedPolicyDocument.setMandatory(true);
		storedPolicyDocument.setRevision(1);

		// System.out.println(Constants.MAPPER.writeValueAsString(storedPolicyDocument));

		AttributesClass attributesClass = new AttributesClass("name", "desc", Set.of("allowed"), Set.of("mandatory"),
				false, Set.of("parent"));
		// System.out.println(Constants.MAPPER.writeValueAsString(attributesClass));

		Message message = new Message("name", new Locale("pl"), "value");
//		System.out.println(Constants.MAPPER.writeValueAsString(message));

		AuthenticationFlowDefinition authenticationFlowDefinition = new AuthenticationFlowDefinition("name",
				Policy.REQUIRE, Set.of("a1"), List.of("a2"));
//		System.out.println(Constants.MAPPER.writeValueAsString(authenticationFlowDefinition));

		ScheduledProcessingRule shProcessingRule = new ScheduledProcessingRule("cond", translationAction, "cron", "id");

		// System.out.println(Constants.MAPPER.writeValueAsString(shProcessingRule));

		CapacityLimit capacityLimit = new CapacityLimit(CapacityLimitName.AttributesCount, 3);
//		System.out.println(Constants.MAPPER.writeValueAsString(capacityLimit));

		CredentialDefinition credentialDefinition = new CredentialDefinition("type", "name");
		credentialDefinition.setConfiguration("config");
		credentialDefinition.setDescription(new I18nString("desc"));
		credentialDefinition.setDisplayedName(new I18nString("disp"));
		credentialDefinition.setReadOnly(true);
//		System.out.println(Constants.MAPPER.writeValueAsString(credentialDefinition));

		CredentialRequirements credentialRequirements = new CredentialRequirements("name", "desc", Set.of("req1"));
		credentialRequirements.setReadOnly(true);
//		System.out.println(Constants.MAPPER.writeValueAsString(credentialRequirements));

		MessageTemplate messageTemplate = new MessageTemplate("name", "desc",
				new I18nMessage(new I18nString("sub"), new I18nString("body")), "consumer", MessageType.PLAIN,
				"channel");
	//	System.out.println(Constants.MAPPER.writeValueAsString(messageTemplate));

		NotificationChannel notificationChannel = new NotificationChannel("name", "desc", "config", "facility");
	//	System.out.println(Constants.MAPPER.writeValueAsString(notificationChannel));

		
		
		
	}
}
