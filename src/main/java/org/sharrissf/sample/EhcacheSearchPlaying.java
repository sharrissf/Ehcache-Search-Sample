package org.sharrissf.sample;

import java.io.IOException;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.aggregator.Average;
import net.sf.ehcache.search.aggregator.Count;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.expression.And;

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
    private Ehcache cache;

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

        sa = new SearchAttribute();
        sa.setExpression("value.getAddress().getState()");
        sa.setName("state");
        cacheConfig.addSearchAttribute(sa);

        cacheManagerConfig.addCache(cacheConfig);

        cacheManager = new CacheManager(cacheManagerConfig);
        cache = cacheManager.getEhcache("test");
    }

    public void runTests() throws IOException {
        loadCache();

        Attribute<Integer> age = cache.getSearchAttribute("age");
        Attribute<Gender> gender = cache.getSearchAttribute("gender");
        Attribute<String> name = cache.getSearchAttribute("name");
        Attribute<String> state = cache.getSearchAttribute("state");

        Query query = cache.createQuery();
        query.includeKeys();
        query.includeValues();
        query.add(new And(name.like("Ari*"), gender.eq(Gender.MALE)));

        long t = System.currentTimeMillis();
        System.out.println("Searching for all Person's who's name start with Ari and are Male:");

        Results results = query.execute();
        System.out.println("Took: " + (System.currentTimeMillis() - t) + " Size: " + results.size());
        System.out.println("----Results-----");
        for (Result result : results.all()) {
            System.out.println("Got: Key[" + result.getKey() + "] Value class [" + result.getValue().getClass() + "] Value ["
                    + result.getValue() + "]");
        }

        read();

        System.out.println("Adding another Ari");

        cache.put(new Element(1, new Person("Ari Eck", 36, Gender.MALE, "eck street", "San Mateo", "CA")));

        t = System.currentTimeMillis();
        System.out.println("Again Searching for all Person's who's name start with Ari and are Male:");
        results = query.execute();
        System.out.println("Took: " + (System.currentTimeMillis() - t) + " Size: " + results.size());

        read();

        System.out.println("Find the average age of all the entries in the cache");

        Query averageAgeQuery = cache.createQuery();
        averageAgeQuery.includeAggregator(new Average(), age);
        System.out.println("Average age: " + averageAgeQuery.execute().aggregateResult());

        read();

        System.out.println("Find the average age of all people between 30 and 40");

        Query agesBetween = cache.createQuery();
        agesBetween.add(age.between(30, 40));
        agesBetween.includeAggregator(new Average(), age);
        System.out.println("Average age between 30 and 40: " + agesBetween.execute().aggregateResult());

        read();

        System.out.println("Find the count of people from NJ");

        Query newJerseyCountQuery = cache.createQuery().add(state.eq("NJ"));
        newJerseyCountQuery.includeAggregator(new Count(), state);
        System.out.println("Count of people from NJ: " + newJerseyCountQuery.execute().aggregateResult());
    }

    private void loadCache() {
        cache.put(new Element(1, new Person("Tim Eck", 35, Gender.MALE, "eck street", "San Mateo", "CA")));
        cache.put(new Element(2, new Person("Pamela Jones", 23, Gender.FEMALE, "berry st", "Parsippany", "LA")));
        cache.put(new Element(3, new Person("Ari Zilka", 25, Gender.MALE, "big wig", "Beverly Hills", "NJ")));
        cache.put(new Element(4, new Person("Ari gold", 45, Gender.MALE, "cool agent", "Madison", "WI")));
        cache.put(new Element(5, new Person("Nabib El-Rahman", 30, Gender.MALE, "dah man", "Bangladesh", "MN")));
        for (int i = 5; i < 1000; i++) {
            cache.put(new Element(i, new Person("Nabib El-Rahman" + i, 30, Gender.MALE, "dah man", "Bangladesh", "NJ")));
        }
    }

    private static void read() throws IOException {
        System.err.println("\nhit enter to continue");
        System.in.read();
    }

    public static void main(String[] args) throws IOException {
        new EhcacheSearchPlaying().runTests();
    }

    public static class NameAttributeExtractor implements AttributeExtractor {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        @Override
        public Object attributeFor(Element element) {
            return ((Person) element.getValue()).getName();
        }

    }
}
