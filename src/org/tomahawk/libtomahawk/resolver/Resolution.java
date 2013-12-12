package org.tomahawk.libtomahawk.resolver;

public class Resolution {

    private Query mQuery;

    private Resolver mResolver;

    public Resolution(Query query, Resolver resolver) {
        mQuery = query;
        mResolver = resolver;
    }

    public Resolver getResolver() {
        return mResolver;
    }

    public Query getQuery() {
        return mQuery;
    }
}
