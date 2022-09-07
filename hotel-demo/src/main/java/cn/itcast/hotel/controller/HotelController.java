package cn.itcast.hotel.controller;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author LEGION
 */
@RestController
@RequestMapping("/hotel")
public class HotelController {
    @Autowired
    private IHotelService hotelService;

    /**
     * 搜索酒店数据
     *
     * @param requestParams
     * @return
     */
    @PostMapping("/list")
    public PageResult search(@RequestBody RequestParams requestParams) {
        return hotelService.search(requestParams);
    }

    /**
     * 酒店过滤
     *
     * @param requestParams
     * @return
     */
    @PostMapping("/filters")
    public Map<String, List<String>> getFilters(@RequestBody RequestParams requestParams) {
        return hotelService.getFilters(requestParams);
    }

    /**
     * 搜索框自动补全
     *
     * @param prefix
     * @return
     */
    @GetMapping("/suggestion")
    public List<String> getSuggestions(@RequestParam("key") String prefix) {
        return hotelService.getSuggestions(prefix);
    }
}
