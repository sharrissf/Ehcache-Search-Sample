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
import net.sf.ehcache.search.attribute.AttributeExtractor;

import org.sharrissf.sample.Person.Gender;

/**
 * Sample app briefly showing some of the api's one can use to search in Ehcache. This has been written against the latest snapshot build so
 * it can become outdated at any time
 * 
 * @author steve
 * 
 */
public class EhcacheSearchPlaying {
    private CacheManager cacheManager;
    private Cache cache;

    public EhcacheSearchPlaying() {
        initializeCache();
    }

    private void initializeCache() {

        // Create Cache
        Configuration cacheManagerConfig = new Configuration();
        cacheManagerConfig.addDefaultCache(new CacheConfiguration());
        CacheConfiguration cacheConfig = new CacheConfiguration("test", -1).eternal(true);

        // Create attributes on the stuff we want to be able to search on.

        // You can use an expression for getting at the value to be indexed on a cache or you can code your own
        SearchAttribute sa = new SearchAttribute();
        sa.setExpression("value.getAge()");
        sa.setName("age");
        cacheConfig.addSearchAttribute(sa);

        sa = new SearchAttribute();
        sa.className("org.sharrissf.sample.EhcacheSearchPlaying$NameAttributeExtractor");

        sa.setName("name");
        cacheConfig.addSearchAttribute(sa);

        sa = new SearchAttribute();
        sa.setExpression("value.getGender()");
        sa.setName("gender");
        cacheConfig.addSearchAttribute(sa);

        cacheManagerConfig.addCache(cacheConfig);

        cacheManager = new CacheManager(cacheManagerConfig);
        cache = cacheManager.getCache("test");
    }

    public void runTests() throws IOException {
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

    public static void main(String[] args) throws IOException {
        new EhcacheSearchPlaying().runTests();
    }

    @SuppressWarnings( { "unused", "serial" })
    public static class NameAttributeExtractor implements AttributeExtractor {

        @Override
        public Object attributeFor(Element element) {
            return ((Person) element.getValue()).getName();
        }

    }
}
