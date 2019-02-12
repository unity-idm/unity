/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.registration.RequestSubmitStatus;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.Selection;

/**
 * MVEL context is a string keyed map. This class adds initialization for registration profile processing.
 * @author Krzysztof Benedyczak
 */
public class RegistrationMVELContext extends HashMap<String, Object>
{
	public enum ContextKey
	{
		idsByType,
		ridsByType,
		idsByTypeObj,
		ridsByTypeObj,
		attrs,
		attr,
		rattrs,
		rattr,
		groups,
		rgroups,
		status,
		triggered,
		onIdpEndpoint,
		userLocale,
		registrationForm,
		requestId,
		agrs,
		validCode;
	}
	
	/**
	 * Setups a full context for {@link EnquiryResponse} or {@link RegistrationRequest} processing
	 */
	public RegistrationMVELContext(BaseForm form, BaseRegistrationInput response,
			RequestSubmitStatus status, TriggeringMode triggered, boolean idpEndpoint, String requestId, 
			AttributeTypeHelper atHelper)
	{
		initCommon(form, response, status, triggered, idpEndpoint, requestId, atHelper);
		put(ContextKey.validCode.name(), response.getRegistrationCode() != null);
	}
	
	private void initCommon(BaseForm form, BaseRegistrationInput request,
			RequestSubmitStatus status, TriggeringMode triggered, boolean idpEndpoint, String requestId, 
			AttributeTypeHelper atHelper)
	{
		createBaseMvelContext(form, status, triggered, idpEndpoint);
		
		put(ContextKey.userLocale.name(), request.getUserLocale());
		put(ContextKey.requestId.name(), requestId);
		
		setupAttributes(form, request, atHelper);
		setupIdentities(form, request);
		setupGroups(form, request);
		setupAgreements(request);
	}
	
	private void setupAttributes(BaseForm form, BaseRegistrationInput request, AttributeTypeHelper atHelper)
	{
		Map<String, Object> attr = new HashMap<>();
		Map<String, List<Object>> attrs = new HashMap<>();
		Map<String, Object> rattr = new HashMap<>();
		Map<String, List<Object>> rattrs = new HashMap<>();
		
		for (int i=0; i<request.getAttributes().size(); i++)
		{
			AttributeRegistrationParam attributeRegistrationParam = form.getAttributeParams().get(i);
			Attribute attribute = request.getAttributes().get(i);
			if (attribute == null)
				continue;
			List<Object> values = attributeValuesAsDomainObjects(attribute.getName(), 
					attribute.getValues(), atHelper);
			Object v = values.isEmpty() ? "" : values.get(0);
			
			attr.put(attribute.getName(), v);
			attrs.put(attribute.getName(), values);

			if (attributeRegistrationParam.getRetrievalSettings().isAutomaticOnly())
			{
				rattr.put(attribute.getName(), v);
				rattrs.put(attribute.getName(), values);				
			}
		}
		put(ContextKey.attr.name(), attr);
		put(ContextKey.attrs.name(), attrs);
		put(ContextKey.rattr.name(), rattr);
		put(ContextKey.rattrs.name(), rattrs);
	}
	
	private List<Object> attributeValuesAsDomainObjects(String attributeName, List<String> values, 
			AttributeTypeHelper atHelper)
	{
		AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntaxForAttributeName(attributeName);
		List<Object> ret = new ArrayList<>(values.size());
		for (String value: values)
			ret.add(syntax.convertFromString(value));
		return ret;
	}
	
	private void setupIdentities(BaseForm form, BaseRegistrationInput request)
	{
		Map<String, List<String>> idsByType = new HashMap<>();
		Map<String, List<String>> ridsByType = new HashMap<>();
		Map<String, List<Object>> idsByTypeObj = new HashMap<>();
		Map<String, List<Object>> ridsByTypeObj = new HashMap<>();
		for (int i=0; i<request.getIdentities().size(); i++)
		{
			IdentityRegistrationParam identityRegistrationParam = form.getIdentityParams().get(i);
			IdentityParam identityParam = request.getIdentities().get(i);
			if (identityParam == null)
				continue;
			List<String> vals = idsByType.get(identityParam.getTypeId());
			if (vals == null)
			{
				vals = new ArrayList<>();
				idsByType.put(identityParam.getTypeId(), vals);
			}
			vals.add(identityParam.getValue());

			List<Object> valsObj = idsByTypeObj.get(identityParam.getTypeId());
			if (valsObj == null)
			{
				valsObj = new ArrayList<>();
				idsByTypeObj.put(identityParam.getTypeId(), valsObj);
			}
			valsObj.add(identityParam);
			
			
			if (identityRegistrationParam.getRetrievalSettings().isAutomaticOnly())
			{
				List<String> rvals = ridsByType.get(identityParam.getTypeId());
				if (rvals == null)
				{
					rvals = new ArrayList<>();
					ridsByType.put(identityParam.getTypeId(), rvals);
				}
				rvals.add(identityParam.getValue());			

				List<Object> rvalsObj = ridsByTypeObj.get(identityParam.getTypeId());
				if (rvalsObj == null)
				{
					rvalsObj = new ArrayList<>();
					ridsByTypeObj.put(identityParam.getTypeId(), rvalsObj);
				}
				rvalsObj.add(identityParam);			
			}
		}

		put(ContextKey.idsByType.name(), idsByType);
		put(ContextKey.ridsByType.name(), ridsByType);
		put(ContextKey.idsByTypeObj.name(), idsByTypeObj);
		put(ContextKey.ridsByTypeObj.name(), ridsByTypeObj);
	}
	
	private void setupGroups(BaseForm form, BaseRegistrationInput request)
	{
		List<String> groups = new ArrayList<>();
		List<String> rgroups = new ArrayList<>();
		for (int i=0; i<request.getGroupSelections().size(); i++)
		{
			GroupRegistrationParam groupRegistrationParam = form.getGroupParams().get(i);
			GroupSelection selection = request.getGroupSelections().get(i);
			if (selection == null)
				continue;
			groups.addAll(selection.getSelectedGroups());
			if (groupRegistrationParam.getRetrievalSettings().isAutomaticOnly())
				rgroups.addAll(selection.getSelectedGroups());
		}
		put(ContextKey.groups.name(), groups);
		put(ContextKey.rgroups.name(), rgroups);
	}
	
	private void setupAgreements(BaseRegistrationInput request)
	{
		ArrayList<String> agr = new ArrayList<String>();
		for (Selection a : request.getAgreements())
		{
			agr.add(Boolean.toString(a.isSelected()));
		}
		put(ContextKey.agrs.name(), agr);
	}
	
	private void createBaseMvelContext(BaseForm form,
			RequestSubmitStatus status, TriggeringMode triggered, boolean idpEndpoint)
	{
		put(ContextKey.onIdpEndpoint.name(), idpEndpoint);
		put(ContextKey.triggered.name(), triggered.toString());
		put(ContextKey.status.name(), status.toString());
		put(ContextKey.registrationForm.name(), form.getName());
		
		Map<String, List<String>> empty = new HashMap<>();
		put(ContextKey.attr.name(), empty);
		put(ContextKey.attrs.name(), empty);
		put(ContextKey.rattr.name(), empty);
		put(ContextKey.rattrs.name(), empty);
		put(ContextKey.idsByType.name(), empty);
		put(ContextKey.ridsByType.name(), empty);
		put(ContextKey.idsByTypeObj.name(), empty);
		put(ContextKey.ridsByTypeObj.name(), empty);
		
		List<String> emptyL = new ArrayList<>();
		put(ContextKey.groups.name(), emptyL);
		put(ContextKey.rgroups.name(), emptyL);
		put(ContextKey.agrs.name(), emptyL);
	}
	
	@Override
	public String toString()
	{
		StringJoiner joiner = new StringJoiner("\n");
		forEach((key, value) -> joiner.add(key + " = " + value));
		return joiner.toString();
	}
}
