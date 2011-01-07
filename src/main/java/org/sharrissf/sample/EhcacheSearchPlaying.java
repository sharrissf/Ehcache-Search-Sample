package org.sharrissf.sample;

import java.io.IOException;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Direction;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.aggregator.Aggregators;
import net.sf.ehcache.search.attribute.AttributeExtractor;

import org.sharrissf.sample.Person.Gender;

/**
 * Sample app briefly showing some of the api's one can use to search in
 * Ehcache. This has been written against the latest snapshot build so it can
 * become outdated at any time
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

		// ***To Cluster With Terracotta***
		// add the ehcache-terracotta-ee jar to your class path and uncomment the
		// code in the "To Cluster With Terracotta" sections
		//
		// TerracottaClientConfiguration tcConfig = new
		// TerracottaClientConfiguration()
		// .url("localhost:9510");
		// cacheManagerConfig.addTerracottaConfig(tcConfig);
		
		// ***To Cluster With Terracotta***

		cacheManagerConfig.addDefaultCache(new CacheConfiguration());

		CacheConfiguration cacheConfig = new CacheConfiguration("test", -1)
				.eternal(true)
		// ***To Cluster With Terracotta***
		// .terracotta(new TerracottaConfiguration())
		// ***To Cluster With Terracotta***
		;
		Searchable searchable = new Searchable();
		cacheConfig.addSearchable(searchable);

		// Create attributes on the stuff we want to be able to search on.

		// You can use an expression for getting at the value to be indexed on a
		// cache or you can code your own

		// Expressions
		// By attribute
		searchable.addSearchAttribute(new SearchAttribute().name("age"));
		// By Expression
		searchable.addSearchAttribute(new SearchAttribute().name("gender")
				.expression("value.getGender()"));
		searchable.addSearchAttribute(new SearchAttribute().name("state")
				.expression("value.getAddress().getState()"));

		// Coding your own extracter
		searchable.addSearchAttribute(new SearchAttribute().name("name")
				.className(NameAttributeExtractor.class.getName()));

		/*
		 * If you want to initialize it via ehcache.xml it would look like this:
		 * 
		 * <cache name="test" maxElementsInMemory="0" eternal="true"
		 * overflowToDisk="false"> <searchable> <searchAttribute name="age"/>
		 * <searchAttribute name="name"
		 * class="org.sharrissf.sample.EhcacheSearchPlaying$NameAttributeExtractor"
		 * /> <searchAttribute name="gender" expression="value.getGender()"/>
		 * <searchAttribute name="state" expression="value.getState()"/>
		 * </searchable> </cache>
		 */

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
		query.addCriteria(name.like("Ari*").and(gender.eq(Gender.MALE)))
				.addOrderBy(age, Direction.ASCENDING).maxResults(10);

		long t = System.currentTimeMillis();
		System.out
				.println("Searching for all Person's who's name start with Ari and are Male:");

		Results results = query.execute();
		System.out.println("Took: " + (System.currentTimeMillis() - t)
				+ " Size: " + results.size());
		System.out.println("----Results-----\n");
		for (Result result : results.all()) {
			System.out
					.println("Got: Key[" + result.getKey() + "] Value class ["
							+ cache.get(result.getKey()).getValue()
							+ "] Value ["
							+ cache.get(result.getKey()).getValue() + "]");
		}

		read();

		System.out.println("Adding another Ari");

		cache.put(new Element(1, new Person("Ari Eck", 36, Gender.MALE,
				"eck street", "San Mateo", "CA")));

		t = System.currentTimeMillis();
		System.out
				.println("Again Searching for all Person's who's name start with Ari and are Male:");
		results = query.execute();
		System.out.println("Took: " + (System.currentTimeMillis() - t)
				+ " Size: " + results.size());

		read();

		System.out
				.println("Find the average age of all the entries in the cache");

		Query averageAgeQuery = cache.createQuery();
		averageAgeQuery.includeAggregator(Aggregators.average(age));
		System.out.println("Average age: "
				+ averageAgeQuery.execute().getAggregatorResults());

		read();

		System.out
				.println("Find the average age of all people between 30 and 40");

		Query agesBetween = cache.createQuery();
		agesBetween.addCriteria(age.between(30, 40));
		agesBetween.includeAggregator(Aggregators.average(age));
		System.out.println("Average age between 30 and 40: "
				+ agesBetween.execute().getAggregatorResults());

		read();

		System.out.println("Find the count of people from NJ");

		Query newJerseyCountQuery = cache.createQuery().addCriteria(
				state.eq("NJ"));
		newJerseyCountQuery.includeAggregator(Aggregators.count());
		System.out.println("Count of people from NJ: "
				+ newJerseyCountQuery.execute().getAggregatorResults());
	}

	private void loadCache() {
		cache.put(new Element(1, new Person("Tim Eck", 35, Gender.MALE,
				"eck street", "San Mateo", "CA")));
		cache.put(new Element(2, new Person("Pamela Jones", 23, Gender.FEMALE,
				"berry st", "Parsippany", "LA")));
		cache.put(new Element(3, new Person("Ari Zilka", 25, Gender.MALE,
				"big wig", "Beverly Hills", "NJ")));
		cache.put(new Element(4, new Person("Ari gold", 45, Gender.MALE,
				"cool agent", "Madison", "WI")));
		cache.put(new Element(5, new Person("Nabib El-Rahman", 30, Gender.MALE,
				"dah man", "Bangladesh", "MN")));
		for (int i = 5; i < 1000; i++) {
			cache.put(new Element(i, new Person("Nabib El-Rahman" + i, 30,
					Gender.MALE, "dah man", "Bangladesh", "NJ")));
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
		 * Implementing the AttributeExtractor Interface and passing it in
		 * allows you to create very efficient and specific attribute extraction
		 * for performance sensitive code
		 */

		public Object attributeFor(Element element) {
			return ((Person) element.getValue()).getName();
		}

	}
}
