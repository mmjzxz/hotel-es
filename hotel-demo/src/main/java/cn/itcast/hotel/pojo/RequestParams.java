package cn.itcast.hotel.pojo;

import lombok.Data;

/**
 * @author LEGION
 */
@Data
public class RequestParams {
    private String key;
    private Integer page;
    private Integer size;
    private String sortBy;
    /**
     * 过滤条件
     */
    private String city;
    private String brand;
    private Integer minPrice;
    private Integer maxPrice;
    private String starName;

    /**
     * 当前位置地理坐标
     */
    private String location;
}
