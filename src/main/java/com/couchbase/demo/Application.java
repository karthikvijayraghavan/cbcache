package com.couchbase.demo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.spring.cache.CacheBuilder;
import com.couchbase.client.spring.cache.CouchbaseCacheManager;

@SpringBootApplication
@EnableCaching
@PropertySources({ @PropertySource("file:${app.home}/app.properties") })
public class Application {

	private static final Logger log = LoggerFactory.getLogger(Application.class);

	public static final String BOOK_CACHE = "books";

	@Value("${nodeList}")
	public String nodeList;

	@Bean(destroyMethod = "disconnect")
	public Cluster cluster() {

		CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
				// this set the IO socket timeout globally, to 60s
				.socketConnectTimeout((int) TimeUnit.SECONDS.toMillis(60)).
				kvTimeout(TimeUnit.SECONDS.toMillis(90))
				.queryTimeout(TimeUnit.SECONDS.toMillis(90))
				/**
				 * this sets the connection timeout for openBucket calls
				 * globally (unless a particular call provides its own timeout)
				 **/
				.connectTimeout(TimeUnit.SECONDS.toMillis(90)).build();

		List<String> nodes = Arrays.asList(nodeList.split("\\s*,\\s*"));
		// Couchbase cluster will be created with the list of nodes specified in
		// the properties file.
		
		return CouchbaseCluster.create(env, nodes);
	}

	@Bean(destroyMethod = "close")
	public Bucket bucket() {
		// this will be the bucket where every cache-related data will be stored
		// note that the bucket "default" must exist
		return cluster().openBucket("default");
	}

	@Bean
	public CacheManager cacheManager() {
		Map<String, CacheBuilder> mapping = new HashMap<String, CacheBuilder>();
		// we'll make this cache manager recognize a single cache named "books"
		mapping.put(BOOK_CACHE, CacheBuilder.newInstance(bucket()));//.withExpirationInMillis(5));
		return new CouchbaseCacheManager(mapping);
	}

	@Component
	static class Runner implements CommandLineRunner {
		@Autowired
		private BookRepository bookRepository;

		@Override
		public void run(String... args) throws Exception {

			log.info(".... Fetching books");
			for (int i = 0; i < args.length; i++) {
				fetchAndLog(args[i]);
			}

		}

		private void fetchAndLog(String isbn) {
			long start = System.currentTimeMillis();
			Book book = bookRepository.getByIsbn(isbn);
			long time = System.currentTimeMillis() - start;
			log.info(isbn + " --> " + book + " in " + time + "ms");
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}