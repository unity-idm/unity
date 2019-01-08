/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.processor;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlObject;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.proto.AssertionResponse;
import eu.unicore.security.dsig.DSigException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.GroupChooser;
import pl.edu.icm.unity.saml.idp.SamlAttributeMapper;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAssertionResponseContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;
import xmlbeans.org.oasis.saml2.assertion.EncryptedAssertionDocument;
import xmlbeans.org.oasis.saml2.assertion.EncryptedElementType;
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
	private AttributeTypeSupport aTypeSupport;
	
	private String chosenGroup;
	private Calendar authnTime;
	
	public BaseResponseProcessor(AttributeTypeSupport aTypeSupport, 
			SAMLAssertionResponseContext<T, C> context, Calendar authnTime)
	{
		super(context);
		this.aTypeSupport = aTypeSupport;
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

	public String getRequestIssuer()
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
	protected Assertion createAttributeAssertion(SubjectType authenticatedOne, Collection<Attribute> attributes) 
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
	protected boolean addAttributesToAssertion(Assertion assertion, Collection<Attribute> attributes) 
			throws SAMLProcessingException
	{
		if (attributes.size() == 0)
			return false;
		SamlAttributeMapper mapper = samlConfiguration.getAttributesMapper();
		List<AttributeType> converted = new ArrayList<AttributeType>(attributes.size());
		for (Attribute attribute: attributes)
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

	
	protected void addAssertionEncrypting(AssertionResponse resp, Assertion assertion) throws SAMLProcessingException
	{
		X509Certificate encCert = samlConfiguration.getEncryptionCertificateForRequester(
				context.getRequest().getIssuer());
		if (encCert != null)
		{
			try
			{
				EncryptedAssertionDocument encrypted = assertion.encrypt(encCert, 128);
				EncryptedElementType at = resp.getXMLBean().addNewEncryptedAssertion();
				at.set(encrypted.getEncryptedAssertion());
			} catch (Exception e)
			{
				throw new SAMLProcessingException("Problem during assertion encryption", e);
			}
		} else
		{
			resp.addAssertion(assertion);
		}
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
			throw new SAMLProcessingException("Signing assertion problem", e);
		}
	}
	
	protected void signResponse(AssertionResponse response) throws SAMLProcessingException
	{
		try
		{
			X509Credential credential = samlConfiguration.getSamlIssuerCredential();
			response.sign(credential.getKey(), credential.getCertificateChain());
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
	
	/**
	 * Assembles all attributes (with translation profiles), then filters them with user controlled preferences.
	 * @param entity
	 * @param processor
	 * @param preferences
	 * @param attributesMan
	 * @param identitiesMan
	 * @return
	 * @throws EngineException
	 */
	public Collection<Attribute> getAttributes(TranslationResult userInfo,
			SPSettings preferences) throws EngineException
	{
		Map<String, Attribute> all = filterSupportedBySamlAttributes(userInfo);
		filterAttributesWithPreferences(preferences, all);
		return all.values();
	}

	/**
	 * Filters the given attributes map with settings from user-controlled preferences
	 * @param preferences
	 * @param all
	 */
	private void filterAttributesWithPreferences(SPSettings preferences, Map<String, Attribute> all)
	{
		Map<String, Attribute> hiddenAttribtues = preferences.getHiddenAttribtues();
		for (Entry<String, Attribute> entry : hiddenAttribtues.entrySet())
		{
			if (!all.containsKey(entry.getKey()))
				continue;
			
			if (entry.getValue() == null)
			{
				all.remove(entry.getKey());
			} else
			{
				Attribute attribute = all.get(entry.getKey());
				List<String> filteredValues = new ArrayList<>();
				for (String value : attribute.getValues())
				{
					if (!findValue(value, entry.getValue()))
						filteredValues.add(value);
				}
				attribute.setValues(filteredValues);
			}
		}
		
		if (log.isDebugEnabled())
			log.debug("Processed attributes to be returned: " + all.values());
	}

	private boolean findValue(String value, Attribute attr)
	{
		AttributeValueSyntax<?> syntax = aTypeSupport.getSyntaxFallingBackToDefault(attr);
		for (String object : attr.getValues())
		{
			if (syntax.areEqualStringValue(value, object))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns a collection of attributes including only those attributes for which there is SAML 
	 * representation.
	 */
	private Map<String, Attribute> filterSupportedBySamlAttributes(TranslationResult userInfo)
	{
		Map<String, Attribute> ret = new HashMap<String, Attribute>();
		SamlAttributeMapper mapper = samlConfiguration.getAttributesMapper();
		
		for (DynamicAttribute da: userInfo.getAttributes())
		{
			Attribute a = da.getAttribute();
			if (mapper.isHandled(a))
				ret.put(a.getName(), a);
		}
		return ret;
	}

	public String getIdentityTarget()
	{
		return context.getRequest().getIssuer().getStringValue();
	}
}
