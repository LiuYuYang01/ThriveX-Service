package liuyuyang.net.dto.project;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class LayoutDTO {
    @ApiModelProperty(value = "文章列表布局", example = "classics")
    private String isArticleLayout;

    @ApiModelProperty(value = "右侧功能栏布局", example = "[\"author\", \"hotArticle\", \"randomArticle\", \"newComments\"]")
    private String rightSidebar;

    @ApiModelProperty(value = "首页背景图", example = "1.jpg")
    private String swiperImage;

    @ApiModelProperty(value = "打字机文本", example = "[\"这是第一段文本\", \"这是第二段文本\", \"...\"]")
    private String swiperText;
}
