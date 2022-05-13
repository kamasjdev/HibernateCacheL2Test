package hibernate.cache.dao;

import org.springframework.stereotype.Repository;

import hibernate.cache.model.Bar;

@Repository
public class BarDao extends AbstractJpaDAO<Bar> implements IBarDao {

    public BarDao() {
        super();

        setClazz(Bar.class);
    }

    // API

}