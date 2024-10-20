package liuyuyang.net.vo;

import io.swagger.annotations.ApiParam;
import lombok.Data;

@Data
public class SortVO {
    @ApiParam(defaultValue = "asc",value = "默认为asc正序排序，如果设置了desc表示降序排序")
    String sort = "desc";
}
