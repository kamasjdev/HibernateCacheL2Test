package hibernate.cache.dao;

import java.util.List;

import hibernate.cache.model.Bar;

public interface IBarDao {

	Bar findOne(long id);

    List<Bar> findAll();

    void create(Bar entity);

    Bar update(Bar entity);

    void delete(Bar entity);

    void deleteById(long entityId);

}