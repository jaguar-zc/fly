package io.sufeng.context.dto.app;

import lombok.Getter;
import lombok.Setter;
import io.sufeng.context.domain.ConversationType;

import java.util.Date;

/**
 * @Author zhangchao
 * @Date 2019/5/28 15:34
 * @Version v1.0
 */
@Getter
@Setter
public class ConversationListDto {

    private String id;
    private ConversationType type;
    private String name;//名称  单聊的话需要查询 动态查询
    private String icon;//图标  单聊的话需要查询 动态查询
    private String tags;//标签
    private Integer top;//置顶
    private Integer dontDisturb;//免打扰
    private String ownerPeopleId;//会话拥有者
    private MessageDto lastMessage;//最后一条消息
    private Date createTime;//创建时间
    private Date lastUpdateTime;//最后更新时间

}
