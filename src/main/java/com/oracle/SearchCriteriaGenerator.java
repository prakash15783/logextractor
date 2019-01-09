package com.oracle;

import java.util.Map;

public class SearchCriteriaGenerator {

    public static SearchCriteria generate(Map<String, String> params){
        SearchCriteria criteria = new SearchCriteria();
        String instanceId = params.get("instanceId");
        if(instanceId != null && !instanceId.isEmpty()) {
            criteria.getSearchTerms().add("[oracle.soa.tracking.InstanceId: " + instanceId + "]");
        }
        String compositeName = params.get("flowName");
        if(compositeName != null && !compositeName.isEmpty()) {
            criteria.getSearchTerms().add("[composite_name: " + compositeName);
        }

        return criteria;
    }



}
