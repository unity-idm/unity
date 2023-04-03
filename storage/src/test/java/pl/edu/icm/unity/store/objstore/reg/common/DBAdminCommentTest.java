/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Date;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBAdminCommentTest extends DBTypeTestBase<DBAdminComment>
{

	@Override
	protected String getJson()
	{
		return "{\"date\":1,\"contents\":\"comment\",\"authorEntityId\":1,\"publicComment\":true}\n";
	}

	@Override
	protected DBAdminComment getObject()
	{
		return DBAdminComment.builder()
				.withAuthorEntityId(1)
				.withContents("comment")
				.withDate(new Date(1))
				.withPublicComment(true)
				.build();
	}

}
