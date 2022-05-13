package persistence.dao.impl;

import persistence.dao.common.AbstractHibernateDao;
import persistence.dao.IFooDao;
import persistence.model.Foo;
import org.springframework.stereotype.Repository;

@Repository
public class FooHibernateDao extends AbstractHibernateDao<Foo> implements IFooDao {

    public FooHibernateDao() {
        super();

        setClazz(Foo.class);
    }

    // API

}