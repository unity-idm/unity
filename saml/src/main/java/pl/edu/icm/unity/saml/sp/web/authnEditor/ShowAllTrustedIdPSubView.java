/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.saml.sp.web.authnEditor;

import java.util.Arrays;
import java.util.List;

import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

public class ShowAllTrustedIdPSubView  extends CustomComponent implements UnitySubView
{

	private UnityMessageSource msg;
	
	
	
	@Override
	public List<String> getBredcrumbs()
	{
		return Arrays.asList(msg.getMessage("EditIndividualTrustedIdpSubView.breadcrumbs"));
	}
}
