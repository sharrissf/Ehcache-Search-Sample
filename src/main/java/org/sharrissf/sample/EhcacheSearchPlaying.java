package org.sharrissf.sample;

import java.io.IOException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.aggregator.Average;

import org.sharrissf.sample.Person.Gender;

public class EhcacheSearchPlaying {

    public static void main(String[] args) throws IOException {

        Configuration cacheManagerConfig = new Configuration();

        // Add default cache
        cacheManagerConfig.addDefaultCache(new CacheConfiguration());

        // Create Cache

        CacheConfiguration cacheConfig = new CacheConfiguration("test", -1).eternal(true);

        SearchAttribute sa = new SearchAttribute();
        sa.setExpression("value.getAge()");
        sa.setName("age");
        cacheConfig.addSearchAttribute(sa);

        sa = new SearchAttribute();
        sa.setExpression("value.getName()");
        sa.setName("name");
        cacheConfig.addSearchAttribute(sa);

        sa = new SearchAttribute();
        sa.setExpression("value.getGender()");
        sa.setName("gender");
        cacheConfig.addSearchAttribute(sa);

        cacheManagerConfig.addCache(cacheConfig);

        CacheManager cacheManager = new CacheManager(cacheManagerConfig);

        Cache cache = cacheManager.getCache("test");

        cache.put(new Element(1, new Person("Tim Eck", 35, Gender.MALE)));
        cache.put(new Element(2, new Person("Pamela Jones", 23, Gender.FEMALE)));
        cache.put(new Element(3, new Person("Ari Zilka", 25, Gender.MALE)));
        cache.put(new Element(4, new Person("Ari gold", 45, Gender.MALE)));
        cache.put(new Element(5, new Person("Nabib El-Rahman", 30, Gender.MALE)));
        for (int i = 5; i < 1000; i++) {
            cache.put(new Element(i, new Person("Nabib El-Rahman" + i, 30, Gender.MALE)));
        }

        Attribute<Integer> age = cache.getSearchAttribute("age");
        Attribute<Gender> gender = cache.getSearchAttribute("gender");
        Attribute<String> name = cache.getSearchAttribute("name");

        Query query = cache.createQuery();
        query.includeKeys();
        query.add(name.like("Ari*"));

        long t = System.currentTimeMillis();
        Results results = query.execute();
        System.out.println("Took: " + (System.currentTimeMillis() - t) + " Size: " + results.size());

        read();

        cache.put(new Element(1, new Person("Tim Eck", 36, Gender.MALE)));

        t = System.currentTimeMillis();

        results = query.execute();
        System.out.println("Took: " + (System.currentTimeMillis() - t) + " Size: " + results.size());

        Query averageAgeQuery = cache.createQuery();
        averageAgeQuery.includeAggregator(new Average(), age);
        System.out.println("Average age: " + averageAgeQuery.execute().aggregateResult());

    }

    private static void read() throws IOException {
        System.err.println("\nhit enter to continue");
        System.in.read();
    }

}
