/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.processor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.proto.AssertionResponse;
import eu.unicore.security.dsig.DSigException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.AttributeFilters;
import pl.edu.icm.unity.saml.idp.GroupChooser;
import pl.edu.icm.unity.saml.idp.SamlAttributeMapper;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties.GroupsSelection;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAssertionResponseContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TransientIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationDataType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationType;
import xmlbeans.org.oasis.saml2.assertion.SubjectType;
import xmlbeans.org.oasis.saml2.protocol.RequestAbstractType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Base class for all processors which return SAML Response. I.e. processors for Authentication and
 * Assertion query protocols.
 * @author K. Benedyczak
 * @param <T>
 * @param <C>
 */
public abstract class BaseResponseProcessor<T extends XmlObject, C extends RequestAbstractType> 
	extends StatusResponseProcessor<T, C>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, BaseResponseProcessor.class);
	
	private String chosenGroup;
	private Calendar authnTime;
	
	public BaseResponseProcessor(SAMLAssertionResponseContext<T, C> context, Calendar authnTime)
	{
		super(context);
		GroupChooser chooser = samlConfiguration.getGroupChooser();
		chosenGroup = chooser.chooseGroup(getRequestIssuer());
		this.authnTime = authnTime;
	}

	public AssertionResponse getOKResponseDocument()
	{
		return new AssertionResponse(getResponseIssuer(), getContext().getRequest().getID());
	}

	public ResponseDocument getErrorResponse(Exception e) 
			throws SAMLProcessingException
	{
		return getErrorResponse(convert2SAMLError(e, null, true));
	}	

	public ResponseDocument getErrorResponse(Exception e, String message) 
			throws SAMLProcessingException
	{
		return getErrorResponse(convert2SAMLError(e, message, false));
	}	

	private String getRequestIssuer()
	{
		NameIDType requestIssuer = getContext().getRequest().getIssuer();
		return requestIssuer.getStringValue();
	}
	
	public ResponseDocument getErrorResponse(SAMLServerException e)
	{
		String id = null;
		C request = getContext().getRequest();
		if (request != null)
			id = request.getID();
		return new AssertionResponse(getResponseIssuer(), id, e).getXMLBeanDoc();
	}

	/**
	 * Returns a collection of attributes including only those attributes for which there is SAML 
	 * representation and which are allowed by attribute filters. The 
	 * 'memberOf' attribute is also added if configured so.
	 * @param allAttribtues, which should be collected in the scope of the chosenGroup. Note that the input list is modified. 
	 * @param allGroups
	 * @return
	 */
	public Map<String, Attribute<?>> prepareReleasedAttributes(Collection<AttributeExt<?>> allAttribtues, 
			Collection<String> allGroups)
	{
		Map<String, Attribute<?>> ret = new HashMap<String, Attribute<?>>();
		Attribute<?> groupAttr = createGroupAttribute(allGroups);
		if (groupAttr != null)
			ret.put(groupAttr.getName(), groupAttr);
		AttributeFilters filter = samlConfiguration.getAttributeFilter();
		filter.filter(allAttribtues, getRequestIssuer());
		
		SamlAttributeMapper mapper = samlConfiguration.getAttributesMapper();
		
		for (AttributeExt<?> a: allAttribtues)
			if (mapper.isHandled(a))
				ret.put(a.getName(), a);
		return ret;
	}

	private Attribute<String> createGroupAttribute(Collection<String> allGroups)
	{
		GroupsSelection mode = samlConfiguration.getEnumValue(SamlIdpProperties.GROUP_SELECTION, 
				SamlIdpProperties.GroupsSelection.class);
		
		String attributeName = samlConfiguration.getValue(SamlIdpProperties.GROUP_ATTRIBUTE);
		List<String> values = new ArrayList<String>();
		switch(mode)
		{
		case none:
			return null;
		case all:
			values.addAll(allGroups);
			break;
		case single:
			if (allGroups.contains(chosenGroup))
				values.add(chosenGroup);
			break;
		case subgroups:
			Group main = new Group(chosenGroup);
			for (String group: allGroups)
			{
				Group g = new Group(group);
				if (g.isChild(main))
					values.add(group);
			}
			break;
		}
		
		return new StringAttribute(attributeName, "/", AttributeVisibility.full, values);
	}

	public String getConfiguredGroupAttribute()
	{
		return samlConfiguration.getValue(SamlIdpProperties.GROUP_ATTRIBUTE); 
	}
	
	public String getChosenGroup()
	{
		return chosenGroup;
	}
	
	public Calendar getAuthnTime()
	{
		return authnTime;
	}
	
	/**
	 * Creates attribute assertion. Returns null when the attributes list is empty
	 * as SAML spec doesn't permit empty attribute assertions. The attributes are filtered using {@link #filterRequested(List)},
	 * which by default does nothing.
	 * @param authenticatedOne
	 * @param attributes
	 * @return
	 * @throws SAMLProcessingException
	 */
	protected Assertion createAttributeAssertion(SubjectType authenticatedOne, Collection<Attribute<?>> attributes) 
			throws SAMLProcessingException
	{
		if (attributes.size() == 0)
			return null;
		Assertion assertion = new Assertion();
		assertion.setIssuer(samlConfiguration.getValue(SamlIdpProperties.ISSUER_URI), 
				SAMLConstants.NFORMAT_ENTITY);
		assertion.setSubject(authenticatedOne);

		if (!addAttributesToAssertion(assertion, attributes))
			return null;
		signAssertion(assertion);
		return assertion;
	}

	/**
	 * Add attributes to an assertion. Do nothing when the attributes list is empty. 
	 * The attributes are filtered using {@link #filterRequested(List)}, which by default does nothing.
	 * @param authenticatedOne
	 * @param attributes
	 * @return whether any attributes were added
	 * @throws SAMLProcessingException
	 */
	protected boolean addAttributesToAssertion(Assertion assertion, Collection<Attribute<?>> attributes) 
			throws SAMLProcessingException
	{
		if (attributes.size() == 0)
			return false;
		SamlAttributeMapper mapper = samlConfiguration.getAttributesMapper();
		List<AttributeType> converted = new ArrayList<AttributeType>(attributes.size());
		for (Attribute<?> attribute: attributes)
		{
			AttributeType samlA = mapper.convertToSaml(attribute);
			converted.add(samlA);
		}
		filterRequested(converted);
		if (converted.size() == 0)
			return false;
		for (AttributeType a: converted)
			assertion.addAttribute(a);
		return true;
	}

	/**
	 * Does nothing. Supclasses which require attribtues filtering should overwrite (as in the case of attribute query protocol).
	 * 
	 * @param converted
	 */
	protected void filterRequested(List<AttributeType> converted)
	{
	}
	
	protected void signAssertion(Assertion assertion) throws SAMLProcessingException
	{
		try
		{
			X509Credential credential = samlConfiguration.getSamlIssuerCredential();
			assertion.sign(credential.getKey(), credential.getCertificateChain());
		} catch (DSigException e)
		{
			throw new SAMLProcessingException("Signing response problem", e);
		}
	}
	
	protected SubjectType cloneSubject(SubjectType src)
	{
		SubjectType ret = SubjectType.Factory.newInstance();
		ret.set(src.copy());
		return ret;
	}

	protected SubjectType setSenderVouchesSubjectConfirmation(SubjectType requested)
	{
		SubjectConfirmationType subConf = SubjectConfirmationType.Factory.newInstance();
		subConf.setMethod(SAMLConstants.CONFIRMATION_SENDER_VOUCHES);
		SubjectConfirmationDataType confData = subConf.addNewSubjectConfirmationData();
		Calendar validity = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		validity.setTimeInMillis(authnTime.getTimeInMillis()+samlConfiguration.getRequestValidity());
		confData.setNotOnOrAfter(validity);
		requested.setSubjectConfirmationArray(new SubjectConfirmationType[] {subConf});
		return requested;
	}
	
	public static Collection<Attribute<?>> getAttributes(EntityParam entity, 
			BaseResponseProcessor<? extends XmlObject, ? extends RequestAbstractType> processor, 
			SPSettings preferences, 
			AttributesManagement attributesMan, IdentitiesManagement identitiesMan) throws EngineException
	{
		Collection<String> allGroups = identitiesMan.getGroups(entity);
		Collection<AttributeExt<?>> allAttribtues = attributesMan.getAttributes(
				entity, processor.getChosenGroup(), null);
		if (log.isTraceEnabled())
			log.trace("Attributes to be returned (before postprocessing): " + 
					allAttribtues + "\nGroups: " + allGroups);
		Map<String, Attribute<?>> all = processor.prepareReleasedAttributes(allAttribtues, allGroups);
		Set<String> hidden = preferences.getHiddenAttribtues();
		for (String hiddenA: hidden)
			all.remove(hiddenA);
		if (log.isDebugEnabled())
			log.debug("Processed attributes to be returned: " + all.values());
		return all.values();
	}
	
	/**
	 * Converts SAML identity format to Unity identity format
	 * @param samlIdFormat
	 * @return
	 * @throws SAMLRequesterException
	 */
	protected String getUnityIdentityFormat(String samlIdFormat) throws SAMLRequesterException
	{
		if (samlIdFormat.equals(SAMLConstants.NFORMAT_PERSISTENT) || 
				samlIdFormat.equals(SAMLConstants.NFORMAT_UNSPEC))
		{
			return TargetedPersistentIdentity.ID;
		} else if (samlIdFormat.equals(SAMLConstants.NFORMAT_DN))
		{
			return X500Identity.ID;
		} else if (samlIdFormat.equals(SAMLConstants.NFORMAT_TRANSIENT))
		{
			return TransientIdentity.ID;
		} else
		{
			throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_INVALID_NAMEID_POLICY,
					samlIdFormat + " is not supported by this service.");
		}
	}
	
	public String getIdentityTarget()
	{
		return context.getRequest().getIssuer().getStringValue();
	}
}
