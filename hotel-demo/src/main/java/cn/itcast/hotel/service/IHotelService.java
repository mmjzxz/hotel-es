package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * @author LEGION
 */
public interface IHotelService extends IService<Hotel> {
    /**
     * 查询酒店信息
     * @param requestParams
     * @return
     */
    PageResult search(RequestParams requestParams);

    /**
     * 酒店过滤信息
     * @param requestParams
     * @return
     */
    Map<String, List<String>> getFilters(RequestParams requestParams);

    /**
     * 搜索框自动补全
     * @param prefix
     * @return
     */
    List<String> getSuggestions(String prefix);
}
