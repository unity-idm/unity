/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.Date;
import java.util.function.Function;

import io.imunity.rest.api.types.registration.RestAdminComment;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.base.registration.AdminComment;

public class AdminCommentMapperTest extends MapperTestBase<AdminComment, RestAdminComment>
{

	@Override
	protected AdminComment getFullAPIObject()
	{
		AdminComment adminComment = new AdminComment("comment", 1, true);
		adminComment.setDate(new Date(1));
		return adminComment;
	}

	@Override
	protected RestAdminComment getFullRestObject()
	{
		return RestAdminComment.builder()
				.withAuthorEntityId(1)
				.withContents("comment")
				.withDate(new Date(1))
				.withPublicComment(true)
				.build();
	}

	@Override
	protected Pair<Function<AdminComment, RestAdminComment>, Function<RestAdminComment, AdminComment>> getMapper()
	{
		return Pair.of(AdminCommentMapper::map, AdminCommentMapper::map);
	}

}
