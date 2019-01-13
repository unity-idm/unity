/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.project;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.mockito.Mock;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;

/**
 * 
 * @author P.Piernik
 *
 */
public class TestProjectBase
{
	@Mock
	protected ProjectAuthorizationManager mockAuthz;

	@Mock
	protected InvitationManagement mockInvitationMan;

	@Mock
	protected GroupsManagement mockGroupMan;

	@Mock
	protected BulkGroupQueryService mockBulkQueryService;

	@Mock
	protected UnityMessageSource mockMsg;

	@Mock
	protected AttributesManagement mockAttrMan;

	@Mock
	protected AttributeTypeManagement mockAttrTypeMan;

	@Mock
	protected AttributesHelper mockAttrHelper;

	@Mock
	protected AttributeTypeHelper mockAtHelper;

	@Mock
	protected EntityManagement mockIdMan;

	@Mock
	protected RegistrationsManagement mockRegistrationMan;

	@Mock
	protected EnquiryManagement mockEnquiryMan;
	
	
	protected  GroupContents getConfiguredGroupContents(String path)
	{
		GroupContents con = new GroupContents();
		Group group = new Group(path);
		group.setDelegationConfiguration(new GroupDelegationConfiguration(true, null, "regForm", "enqForm",
				"stickyReqForm", Lists.emptyList()));
		con.setGroup(group);
		return con;
	}

	protected void assertExceptionType(Throwable exception, Class<?> type)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(type);
	}
}
