package io.sufeng.context.dto.app;

import lombok.Getter;
import lombok.Setter;
import io.sufeng.context.domain.Language;
import io.sufeng.context.domain.entity.PeopleSex;

import java.util.Date;

/**
 * @Author zhangchao
 * @Date 2019/5/27 17:14
 * @Version v1.0
 */
@Getter
@Setter
public class PeopleInfoDto {

    private String id;
    private String peopleNo;
    private Date createTime;
    private String encodedPrincipal;
    private String nickName;
    private String phone;
    private PeopleSex sex;
    private Language language = Language.zh_CN;
    private String country;
    private String province;
    private String city;
    private String introduction;
    private Integer peopleAssistCount;

}
