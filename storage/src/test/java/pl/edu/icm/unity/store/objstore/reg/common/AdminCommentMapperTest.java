/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Date;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.registration.AdminComment;

public class AdminCommentMapperTest extends MapperTestBase<AdminComment, DBAdminComment>
{

	@Override
	protected AdminComment getFullAPIObject()
	{
		AdminComment adminComment = new AdminComment("comment", 1, true);
		adminComment.setDate(new Date(1));
		return adminComment;
	}

	@Override
	protected DBAdminComment getFullDBObject()
	{
		return DBAdminComment.builder()
				.withAuthorEntityId(1)
				.withContents("comment")
				.withDate(new Date(1))
				.withPublicComment(true)
				.build();
	}

	@Override
	protected Pair<Function<AdminComment, DBAdminComment>, Function<DBAdminComment, AdminComment>> getMapper()
	{
		return Pair.of(AdminCommentMapper::map, AdminCommentMapper::map);
	}

}
