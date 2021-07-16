/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.policyAgreement;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.webui.wellknownurl.PublicViewProvider;

@Component
public class PolicyDocumentViewProvider implements PublicViewProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, PolicyDocumentViewProvider.class);

	private ObjectFactory<PublicPolicyDocumentView> viewFactory;
	private PolicyDocumentManagement policyDocMan;

	PolicyDocumentViewProvider(ObjectFactory<PublicPolicyDocumentView> viewFactory,
			@Qualifier("insecure") PolicyDocumentManagement policyDocMan)
	{
		this.viewFactory = viewFactory;
		this.policyDocMan = policyDocMan;
	}

	@Override
	public String getViewName(String viewAndParameters)
	{
		String docId = getDocId(viewAndParameters);
		if (docId == null)
			return null;

		return viewAndParameters;
	}

	private String getDocId(String viewAndParameters)
	{
		if (PublicPolicyDocumentView.POLICY_DOC_VIEW.equals(viewAndParameters))
		{
			VaadinRequest currentRequest = VaadinService.getCurrentRequest();
			if (currentRequest == null)
				return null;
			return currentRequest.getParameter(PublicPolicyDocumentView.POLICY_DOC_PARAM);
		}
		return null;
	}

	@Override
	public View getView(String viewName)
	{
		String docId = getDocId(viewName);
		if (docId == null)
			return null;
		PolicyDocumentWithRevision doc = getDoc(docId);
		if (doc == null)
			return null;
		return viewFactory.getObject().init(doc);
	}

	private PolicyDocumentWithRevision getDoc(String docId)
	{
		try
		{
			return policyDocMan.getPolicyDocument(Long.valueOf(docId));
		} catch (Exception e)
		{
			log.error("Unknown policy document id", e);
			return null;
		}
	}
}
