package com.imooc.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.checkerframework.checker.units.qual.min;
import org.hibernate.validator.constraints.Range;



@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class VlogBO {

    private String id;
    @NotBlank(message = "用户不存在")
    private String vlogerId;
    @Pattern(regexp = "/^((ht|f)tps?):\\/\\/[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:\\/~\\+#]*[\\w\\-\\@?^=%&\\/~\\+#])?$/;\n")
    private String url;
    @NotBlank(message = "视频封面不能为空")
    @Pattern(regexp = "/^((ht|f)tps?):\\/\\/[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:\\/~\\+#]*[\\w\\-\\@?^=%&\\/~\\+#])?$/;\n")
    private String cover;
    private String title;
    @Min(100)
    private Integer width;
    @Min(100)
    private Integer height;
    private Integer likeCounts;
    private Integer commentsCounts;

}
