package com.sparkcleancebu.http_helper;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class HttpHeaders extends ArrayList<HttpHeaders.Header> {
    // Constructor to initialize the base ArrayList
    public HttpHeaders() {
        super();
    }

    // Adds a new header to the list
    public HttpHeaders add(String name, String value) {
        this.add(new Header(name, value)); // Uses the inherited add method from ArrayList
        return this; // For method chaining
    }
    
    // Updates an existing header by name or adds a new one if not found
    public HttpHeaders update(String name, String value) {
        for (Header header : this) {
            if (header.getName().equalsIgnoreCase(name)) { // Case-insensitive header name comparison
                header.setValue(value);  // Update existing value
                return this; // Return for method chaining
            }
        }
        // If not found, add a new header
        this.add(name, value);
        return this;
    }

    // Clears all headers from the list
    public void clear() {
        super.clear(); // Calls ArrayList's clear() method
    }

    // Header class to represent a single header
    public static class Header {
        private String name;
        private String value;

        public Header(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
        
        public void setName(String name) {
        	this.name = name;
        }
        
        public void setValue(String value) {
        	this.value = value;
        }

        @Override
        public String toString() {
            return name + ": " + value;
        }
    }

    // Override toString to represent all headers as a string
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Header header : this) {
            builder.append(header.toString()).append("\n");
        }
        return builder.toString();
    }
}
