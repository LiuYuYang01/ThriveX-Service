package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("config")
public class Config {
    @ApiModelProperty(value = "defaultDarkTheme", example = "配置名称", required = true)
    private String name;

    @ApiModelProperty(value = "true", example = "配置值", required = true)
    private String value;

    @TableField("`group`")
    @ApiModelProperty(value = "layout", example = "分组", required = true)
    private String group;

    @ApiModelProperty(value = "默认是否开启暗黑模式", example = "备注")
    private String note;
}
