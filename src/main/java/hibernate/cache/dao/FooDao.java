package hibernate.cache.dao;

import org.springframework.stereotype.Repository;

import hibernate.cache.model.Foo;

@Repository
public class FooDao extends AbstractJpaDAO<Foo> implements IFooDao {

    public FooDao() {
        super();

        setClazz(Foo.class);
    }

    // API

}
