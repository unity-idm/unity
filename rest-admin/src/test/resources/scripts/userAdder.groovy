import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.base.identity.EntityState
import pl.edu.icm.unity.base.identity.IdentityParam


log.info("Context is {}", context);
IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, context);
entityManagement.addEntity(toAdd, EntityState.valid);
