package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LEGION
 */
@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
    private final static String BRAND_AGGREGATION = "brandAgg";
    private final static String CITY_AGGREGATION = "cityAgg";
    private final static String STAR_AGGREGATION = "starAgg";
    private final static String HOTEL_INDEX = "hotel";

    @Autowired
    private RestHighLevelClient client;

    @Override
    public PageResult search(RequestParams requestParams) {
        try {
            SearchRequest request = new SearchRequest(HOTEL_INDEX);
            buildBasicQuery(requestParams, request);
            //分页
            int page = requestParams.getPage();
            int size = requestParams.getSize();
            request.source().from((page - 1) * size).size(size);

            //排序
            String location = requestParams.getLocation();
            if (location != null && !location.equals("")) {
                request.source().sort(SortBuilders
                        .geoDistanceSort("location", new GeoPoint(location))
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS));
            }

            //发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, List<String>> getFilters(RequestParams requestParams) {
        try {
            SearchRequest request = new SearchRequest(HOTEL_INDEX);
            buildBasicQuery(requestParams, request);
            request.source().size(0);
            buildAggregation(request);
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            Map<String, List<String>> result = new HashMap<>();
            Aggregations aggregations = response.getAggregations();
            List<String> brandList = getAggByName(aggregations, BRAND_AGGREGATION);
            result.put("brand", brandList);
            List<String> cityList = getAggByName(aggregations, CITY_AGGREGATION);
            result.put("city", cityList);
            List<String> starList = getAggByName(aggregations, STAR_AGGREGATION);
            result.put("starName", starList);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取补全信息
     * @param prefix
     * @return
     */
    @Override
    public List<String> getSuggestions(String prefix) {
        try {
            SearchRequest request = new SearchRequest("hotel");
            request.source().suggest(new SuggestBuilder().addSuggestion(
                    "suggestions",
                    SuggestBuilders.completionSuggestion("suggestion")
                            .prefix(prefix)
                            .skipDuplicates(true)
                            .size(10)
            ));
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //解析response
            Suggest suggest = response.getSuggest();
            CompletionSuggestion suggestions = suggest.getSuggestion("suggestions");
            List<CompletionSuggestion.Entry.Option> options = suggestions.getOptions();
            List<String> list = new ArrayList<>(options.size());
            for (CompletionSuggestion.Entry.Option option : options) {
                String text = option.getText().toString();
                list.add(text);
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private List<String> getAggByName(Aggregations aggregations, String aggName) {
        Terms terms = aggregations.get(aggName);
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        List<String> resultList = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            String key = bucket.getKeyAsString();
            resultList.add(key);
        }
        return resultList;
    }

    private void buildAggregation(SearchRequest request) {
        request.source().aggregation(AggregationBuilders
                .terms(BRAND_AGGREGATION)
                .field("brand")
                .size(100));
        request.source().aggregation(AggregationBuilders
                .terms(CITY_AGGREGATION)
                .field("city")
                .size(100));
        request.source().aggregation(AggregationBuilders
                .terms(STAR_AGGREGATION)
                .field("starName")
                .size(100));
    }

    private void buildBasicQuery(RequestParams requestParams, SearchRequest request) {
        //构建BoolQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        String key = requestParams.getKey();
        if (key == null || "".equals(key)) {
            boolQuery.must(QueryBuilders.matchAllQuery());
        } else {
            boolQuery.must(QueryBuilders.matchQuery("all", key));
        }
        //城市条件
        if (requestParams.getCity() != null && !requestParams.getCity().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("city", requestParams.getCity()));
        }
        //品牌
        if (requestParams.getBrand() != null && !requestParams.getBrand().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("brand", requestParams.getBrand()));
        }
        //星级
        if (requestParams.getStarName() != null && !requestParams.getStarName().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("starName", requestParams.getStarName()));
        }
        //价格
        if (requestParams.getMinPrice() != null && requestParams.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders
                    .rangeQuery("price")
                    .gte(requestParams.getMinPrice())
                    .lte(requestParams.getMaxPrice())
            );
        }
        //广告,算分控制
        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
                //原始查询,相关性算分查询
                boolQuery,
                //function score的数组
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                //过滤条件
                                QueryBuilders.termQuery("isAD", true),
                                ScoreFunctionBuilders.weightFactorFunction(10)
                        )
                }
        );
        request.source().query(functionScoreQuery);
    }

    private PageResult handleResponse(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        //获取总数据
        long total = searchHits.getTotalHits().value;
        //文档数组
        SearchHit[] hits = searchHits.getHits();
        List<HotelDoc> hotels = new ArrayList<>();
        for (SearchHit hit : hits) {
            //获取文档source
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            Object[] sortValues = hit.getSortValues();
            if (sortValues.length > 0) {
                Object sortValue = sortValues[0];
                hotelDoc.setDistance(sortValue);
            }
            hotels.add(hotelDoc);
        }
        return new PageResult(total, hotels);
    }
}
