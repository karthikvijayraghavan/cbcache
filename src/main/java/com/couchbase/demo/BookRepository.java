package com.couchbase.demo;




public interface BookRepository {

    Book getByIsbn(String isbn);

}
