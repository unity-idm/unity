/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.saml.processor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.proto.AssertionResponse;
import eu.unicore.security.dsig.DSigException;

import pl.edu.icm.unity.samlidp.AttributeFilters;
import pl.edu.icm.unity.samlidp.GroupChooser;
import pl.edu.icm.unity.samlidp.SamlAttributeMapper;
import pl.edu.icm.unity.samlidp.SamlProperties;
import pl.edu.icm.unity.samlidp.SamlProperties.GroupsSelection;
import pl.edu.icm.unity.samlidp.saml.SAMLProcessingException;
import pl.edu.icm.unity.samlidp.saml.ctx.SAMLAssertionResponseContext;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
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
		GroupsSelection mode = samlConfiguration.getEnumValue(SamlProperties.GROUP_SELECTION, 
				SamlProperties.GroupsSelection.class);
		
		String attributeName = samlConfiguration.getValue(SamlProperties.GROUP_ATTRIBUTE);
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
	 * as SAML spec doesn't permit empty attribute assertions. 
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
		assertion.setIssuer(samlConfiguration.getValue(SamlProperties.ISSUER_URI), 
				SAMLConstants.NFORMAT_ENTITY);
		assertion.setSubject(authenticatedOne);
		
		SamlAttributeMapper mapper = samlConfiguration.getAttributesMapper();
		for (Attribute<?> attribute: attributes)
		{
			AttributeType samlA = mapper.convertToSaml(attribute);
			assertion.addAttribute(samlA);
		}
		
		signAssertion(assertion);
		return assertion;
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
		Calendar validity = Calendar.getInstance();
		validity.setTimeInMillis(authnTime.getTimeInMillis()+samlConfiguration.getRequestValidity());
		confData.setNotOnOrAfter(validity);
		requested.setSubjectConfirmationArray(new SubjectConfirmationType[] {subConf});
		return requested;
	}
}
