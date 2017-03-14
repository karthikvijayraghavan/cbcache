package com.couchbase.demo;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository("book_repo")
public class SimpleBookRepository implements BookRepository {

	@Value("${tableContent}")
	private String cacheData;

	private static final Logger log = LoggerFactory.getLogger(SimpleBookRepository.class);

	@Override
	@Cacheable(value =Application.BOOK_CACHE, unless = "#result == null" )
	public Book getByIsbn(String isbn) {
		simulateSlowService();
		// In real world scenario this may be from a SQL Or Oracle Server DB
		List<String> cacheDataItems = Arrays.asList(cacheData.split("\\s*,\\s*"));
		Iterator<String> cacheDataItemsIterator = cacheDataItems.iterator();
		while (cacheDataItemsIterator.hasNext()) {
			// This represents individual record (row) in the table.
			String cacheDataItem = cacheDataItemsIterator.next();
			List<String> cacheDataItemData = Arrays.asList(cacheDataItem.split("\\s*:\\s*"));
			if (isbn!=null && isbn.equalsIgnoreCase(cacheDataItemData.get(0))) {

				Book result = new Book(isbn, cacheDataItemData.get(1));
				
				return result;
			}
		}

		return null;
	}

	// Don't do this at home
	private void simulateSlowService() {
		try {
			long time = 5000L;
			Thread.sleep(time);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

}
