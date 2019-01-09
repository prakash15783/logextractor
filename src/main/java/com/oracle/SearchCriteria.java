package com.oracle;

import java.util.ArrayList;
import java.util.List;

public class SearchCriteria {
    private List<String> searchTerms = new ArrayList<String>();

    public List<String> getSearchTerms() {
        return searchTerms;
    }

    public void setSearchTerms(List<String> searchTerms) {
        this.searchTerms = searchTerms;
    }
}
