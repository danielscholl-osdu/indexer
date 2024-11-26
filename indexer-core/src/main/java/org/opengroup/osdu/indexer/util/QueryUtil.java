package org.opengroup.osdu.indexer.util;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class QueryUtil {
    public static Query createQueryForParentConfigs(String childKind) {
        String queryString =
                String.format("data.Configurations.Paths.RelatedObjectsSpec.RelationshipDirection: ParentToChildren AND data.Configurations.Paths.RelatedObjectsSpec.RelatedObjectKind:\"%s\"", childKind);
        return createQueryForRelatedConfig(queryString);
    }

    public static Query createQueryForChildrenConfigs(String parentKind) {
        String queryString =
                String.format("data.Configurations.Paths.RelatedObjectsSpec.RelationshipDirection: ChildToParent AND data.Configurations.Paths.RelatedObjectsSpec.RelatedObjectKind:\"%s\"", parentKind);
        return createQueryForRelatedConfig(queryString);
    }

    public static Query createQueryForConfigurations(String kind) {
        Query singleQuery = createSimpleTextQuery(String.format("data.Code: \"%s\"", kind));
        String nestedQueryString = String.format("data.Configurations.Paths.RelatedObjectsSpec.RelatedObjectKind:\"%s\"", kind);
        Query nestedQuery = createQueryForRelatedConfig(nestedQueryString);

        BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
        boolQueryBuilder.boost(1.0F);
        boolQueryBuilder.should(singleQuery);
        boolQueryBuilder.should(nestedQuery);
        return new Query.Builder().bool(boolQueryBuilder.build()).build();
    }

    public static Query createSimpleTextQuery(String queryString) {
        QueryStringQuery.Builder queryStringQueryBuilder = getQueryStringQueryBuilder(queryString);
        Query.Builder queryBuilder = (Query.Builder) new Query.Builder().queryString(queryStringQueryBuilder.build());
        return queryBuilder.build();
    }

    public static String createIdsFilter(List<String> ids) {
        StringBuilder idsBuilder = new StringBuilder();
        for (String id : ids) {
            if (!idsBuilder.isEmpty()) {
                idsBuilder.append(" OR ");
            }
            idsBuilder.append("\"");
            idsBuilder.append(PropertyUtil.removeIdPostfix(id));
            idsBuilder.append("\"");
        }
        return idsBuilder.toString();
    }

    public static List<SortOptions> createSortOptionsList(List<String> fields, List<SortOrder> orders) throws Exception {
        if(fields == null || fields.isEmpty() || orders == null ||  orders.isEmpty()) {
            return null;
        }
        if(fields.size() != orders.size()) {
            throw new Exception("The size of the fields is not matched to the size of the orders");
        }

        List<SortOptions> sortOptionsList = new ArrayList<>();
        for(int i = 0; i < fields.size(); i++) {
            // If field is a text property under data block, make sure that the field is ended with ".keyword"
            // e.g. "data.Code.keyword instead of "data.Code"
            String field = fields.get(i);
            SortOrder order = orders.get(i);
            SortOptions sortOptions = new SortOptions.Builder()
                    .field(f ->f.field(field)
                                .order(order)
                                .missing("_last")
                                .unmappedType(FieldType.Keyword))
                    .build();
            sortOptionsList.add(sortOptions);
        }

        return sortOptionsList;
    }

    private static QueryStringQuery.Builder getQueryStringQueryBuilder(String queryString) {
        if(StringUtils.isBlank(queryString)) {
            queryString = "*";
        }
        return new QueryStringQuery.Builder()
                .query(queryString)
                .fields(new ArrayList<>())
                .type(TextQueryType.BestFields)
                .defaultOperator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
                .maxDeterminizedStates(10000)
                .allowLeadingWildcard(false)
                .enablePositionIncrements(true)
                .fuzziness("AUTO")
                .fuzzyPrefixLength(0)
                .fuzzyMaxExpansions(50)
                .phraseSlop(0.0)
                .escape(false)
                .autoGenerateSynonymsPhraseQuery(true)
                .fuzzyTranspositions(true)
                .boost(1.0f);
    }

    private static Query createQueryForRelatedConfig(String queryString) {
        Query simpleTextQuery = createSimpleTextQuery(queryString);
        Query.Builder innerNestedBuilder = (Query.Builder) new Query.Builder().nested(
                n ->n.path("data.Configurations.Paths")
                        .query(simpleTextQuery)
                        .boost(1.0f)
                        .ignoreUnmapped(true)
                        .scoreMode(ChildScoreMode.Avg));
        Query.Builder nestedBuilder = (Query.Builder) new Query.Builder().nested(
                n ->n.path("data.Configurations")
                        .query(innerNestedBuilder.build())
                        .boost(1.0f)
                        .ignoreUnmapped(true)
                        .scoreMode(ChildScoreMode.Avg));
        return nestedBuilder.build();
    }
}
